package Document;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

class Fat
{
	final static int totalBlock=10;		
	int[] useBlock=new int[totalBlock];		
	Block getBlock()
	{
		for (int i=0; i<totalBlock; i++)	
		{
			if (useBlock[i]!=(1<<totalBlock)-1)	//����λ����������Ч�ʣ����һ���ж�10��block�Ƿ�Ϊ��
			{
				for (int k=0; k<10; k++)
					if ( ( useBlock[i] & (1<<k) )==0)
					{
						Block block=new Block();
						MyHome.block[i*totalBlock+k]=block;
						block.index=i*totalBlock+k;
						return block;
					}
			}
		}
		return null;
	}
	
	void setUseBlock(int index)
	{
		useBlock[index/totalBlock]=useBlock[index/totalBlock] | (1<<index%10);
	}
	
	void deleteBlock(FCB file)
	{
		
		useBlock[file.block.index/totalBlock]=useBlock[file.block.index/totalBlock] & (~(1<<file.block.index%10));
		MyHome.block[file.block.index]=null;
		file.block=null;
	}
}


public class MyHome 
{
	
	static JFrame mainFrame;
	static JPanel mainPanel;
	static JLabel diskName=new JLabel("�ҵĴ���",JLabel.CENTER);
	static DiskPanel diskPanel;
	static ContentPanel contentPanel;
	static Fat fat=new Fat();
	static Block []block=new Block[1000];
	
	static JPanel getMainPanel()
	{
		return mainPanel;
	}
	
	static DiskPanel getDiskPanel()
	{
		return diskPanel;
	}
	static class DiskPanel extends JPanel
	{
		JPanel diskViewPanel;
		void setFolderPanel()
		{
			diskViewPanel=new JPanel();
			diskViewPanel.setBackground(Color.white);
			diskViewPanel.setBounds(15, 5, 70, 70);
			diskViewPanel.addMouseListener(diskMouseListener);
			ImageIcon img = new ImageIcon("resource/disk.png");
			JLabel imgLabel = new JLabel(img);
			diskViewPanel.add(imgLabel);
			add(diskViewPanel);
		}
		

		DiskPanel()
		{
			setBackground(Color.white);
			setLayout(null);
			setPreferredSize(new Dimension(100,100));
			setFolderPanel();	
			diskName.setBounds(10, 80, 80, 20);
			add(diskName);
		}
		
		MouseListener diskMouseListener=new MouseListener()
		{

			public void mouseClicked(MouseEvent e) 
			{
				if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount()==2)
				{
					diskViewPanel.setBackground(Color.white);
					open();
				}
				if (e.getButton()==MouseEvent.BUTTON3)
				{
					JPopupMenu menu=new JPopupMenu();
					JMenuItem openMenu=new JMenuItem("��");
					openMenu.addActionListener(openMenuListener);
					menu.add(openMenu);
					
					JMenuItem deleteAll=new JMenuItem("��ʽ��");
					deleteAll.addActionListener(deleteAllMenuListener);
					menu.add(deleteAll);
					
					menu.show(e.getComponent(),e.getX(),e.getY());
				}
			}
			
			public void mouseEntered(MouseEvent arg0) 
			{
				diskViewPanel.setBackground(Color.gray);
			}

			public void mouseExited(MouseEvent arg0) 
			{
				diskViewPanel.setBackground(Color.white);
			}

			public void mousePressed(MouseEvent arg0) 
			{
			}

			public void mouseReleased(MouseEvent arg0) 
			{
			}
			
			ActionListener openMenuListener = new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					open();
				}				
			}; 
			
			ActionListener deleteAllMenuListener = new ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					delete();
				}				
			}; 
			
		};
		
	}
	
	public MyHome()
	{
		if (mainFrame==null)
		{
			createNewDisk();
		}
	}
	
	void createNewDisk()
	{
		mainFrame=new JFrame();
		mainPanel=new JPanel();
		
		mainFrame.addWindowListener(saveExitListener);
		mainFrame.setVisible(true);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(new MyToolBar().panel,"North");
		mainFrame.add(mainPanel,"Center");
		
		mainPanel.setLayout(new GridLayout(1,1));
			
		diskPanel=new DiskPanel();
		contentPanel=new ContentPanel(contentPanel);
		
		mainPanel.add(diskPanel);
		mainFrame.setSize(1600,950);
		mainFrame.setLocationRelativeTo(null);
		
		MyToolBar.getToolBar().setAddress("�ҵĴ���");
		
		if (new File("OS_File_3.bin").exists())	openFile();
		else
		{
			block[0]=fat.getBlock();
			block[0].property="12";
			fat.setUseBlock(0);
		}
	}
	static void open()
	{
		ContentPanel.switchPanel(contentPanel);
	}
	
	static void delete()
	{
		int option = JOptionPane.showConfirmDialog(
			null, "��ʽ����",
			"�Ƿ��ʽ��", JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE, null);  
		if (option==JOptionPane.NO_OPTION)	return ;
		
		boolean isDelete=true;
			
		for (int i=0; i<contentPanel.fileList.size(); i++)
		{
			if (!contentPanel.fileList.get(i).delete(false)) isDelete=false;
			else i--;
		}
		for (int i=0; i<contentPanel.folderList.size(); i++)
		{
			if (!contentPanel.folderList.get(i).delete(false)) isDelete=false;
			else i--;
		}
		
		if (isDelete)
		{
			MyToolBar.getToolBar().setAddress("�ҵĴ���");
			JOptionPane.showMessageDialog(null, "��ʽ���ɹ���");	
		}
	}
	
	void openFile()
	{
		try
		{
			ObjectInputStream in=new ObjectInputStream(new FileInputStream(new File("OS_File_3.bin")));
			String []blockProperty=(String[])in.readObject();
			String []blockData=(String[])in.readObject();
			int []blockIndex=(int[])in.readObject();
			for (int i=0; i<1000; i++)
			{
				if (blockProperty[i]!=null)
				{
					block[i]=new Block();
					block[i].property=blockProperty[i];
					block[i].data=blockData[i];
					block[i].index=blockIndex[i];
					fat.setUseBlock(block[i].index);
				}
			}
			in.close();
			
			String str=block[0].data;
			while (true)		//�ļ���
			{
				int index=str.indexOf('\n');
				if (str.substring(0, index).equals("NULL"))
				{
					str=str.substring(index+1);
					break;
				}
				int end=new Integer(str.substring(0,index)).intValue();
				new Folder(MyHome.block[end],MyHome.contentPanel);
				str=str.substring(index+1);
			}
			while (!str.isEmpty())
			{
				int index=str.indexOf('\n');
				int end=new Integer(str.substring(0,index)).intValue();
				new MyText(MyHome.block[end],MyHome.contentPanel);
				str=str.substring(index+1);
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	void saveBlock()
	{
		
		try 
    	{
			ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(new File("OS_File_3.bin")));
			String []blockProperty=new String[1000];
			String []blockData=new String[1000];
			int []blockIndex=new int[1000];
			for (int i=0; i<1000; i++)
			{
				if (block[i]!=null)
				{
					blockProperty[i]=block[i].property;
					blockData[i]=block[i].data;
					blockIndex[i]=block[i].index;
				}
				else
				{
					blockProperty[i]=null;
					blockData[i]=null;
					blockIndex[i]=0;
				}
			}
			out.writeObject(blockProperty);
			out.writeObject(blockData);
			out.writeObject(blockIndex);
			out.close();
		} 
    	catch (FileNotFoundException e1) 
		{
			e1.printStackTrace();
		} 
    	catch (IOException e1) 
		{
			e1.printStackTrace();
		} 
	}
	WindowAdapter saveExitListener=new WindowAdapter() 
	{ 
		public void windowClosing(WindowEvent   e) 
	    {     
			saveBlock();
	    }
	 };

}
