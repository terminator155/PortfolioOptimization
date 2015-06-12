import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.GroupLayout;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.JProgressBar;
import javax.swing.JLabel;


public class PortfolioFrame extends JFrame implements ActionListener,OptiProgressIndicator {
	JPanel panel;
	GroupLayout layout;
	
	JPanel panelInput = new JPanel();
	JFileChooser inputChooser = null;
	JTextField inputPathField;
	JButton inputPathButton;
	
	JTable tableStats;
	JTable tableOpti;
	
	JButton buttonExecSelection;
	JButton buttonExecOpti;
	List<StockEntry> efficientStocks = null;
	
	JProgressBar progressBar;
	JLabel labelStaticProg;
	JLabel labelProgress;
	
	public PortfolioOpti optimizer;
	
	public PortfolioFrame()
	{
		super("Optimisation de Portefeuille");
		this.setBounds(100, 100, 400, 400);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.layout = new GroupLayout(this.getContentPane());
		this.layout.setAutoCreateGaps(true);
		this.layout.setAutoCreateContainerGaps(true);
		this.getContentPane().setLayout(layout);
		
		this.initializeUI();
		
		this.pack();
		this.setVisible(true);
	}
	
	public void initializeUI()
	{
		this.inputPathButton = new JButton("1. Select input directory");
		//this.panel.add(this.inputPathButton);
		this.inputPathButton.addActionListener(this);
		
		this.inputPathField = new JTextField();
		//this.inputPathField.setBounds(20, 50, 100, 20);
		//this.panel.add(inputPathField);
		
		this.buttonExecSelection = new JButton("2. Analyse des bourses");
		//this.panel.add(buttonExecSelection);
		this.buttonExecSelection.addActionListener(this);
		
		this.tableStats = new JTable(new StatsTableModel());
		//this.tableStats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tablePane = new JScrollPane(this.tableStats);
		
		this.buttonExecOpti = new JButton("3. Optimiser la portefeuille");
		this.buttonExecOpti.addActionListener(this);
		this.tableOpti = new JTable(new OptiTableModel());
		JScrollPane optiPane = new JScrollPane(this.tableOpti,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.tableOpti.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		this.progressBar = new JProgressBar();
		this.progressBar.setStringPainted(true);
		this.labelProgress = new JLabel("Idel");
		this.labelStaticProg = new JLabel("Progress:");
		
		this.layout.setHorizontalGroup(
				this.layout.createSequentialGroup()
				.addGroup(this.layout.createParallelGroup()
						.addComponent(inputPathButton)
						.addComponent(inputPathField)
						.addComponent(buttonExecSelection)
						.addComponent(tablePane))
				.addGroup(this.layout.createParallelGroup()
						.addGroup(this.layout.createSequentialGroup()
								.addComponent(labelStaticProg)
								.addComponent(progressBar))
						.addComponent(labelProgress)
						.addComponent(this.buttonExecOpti)
						.addComponent(optiPane)));
		
		this.layout.setVerticalGroup(
				this.layout.createSequentialGroup()
				.addGroup(this.layout.createParallelGroup()
						.addComponent(inputPathButton)
						.addGroup(this.layout.createParallelGroup()
								.addComponent(labelStaticProg)
								.addComponent(progressBar)))
				.addGroup(this.layout.createParallelGroup()
						.addComponent(inputPathField)
						.addComponent(labelProgress))
				.addGroup(this.layout.createParallelGroup()
						.addComponent(buttonExecSelection)
						.addComponent(buttonExecOpti))
				.addGroup(this.layout.createParallelGroup()
						.addComponent(tablePane).addComponent(optiPane)));
	}
	
	public JFileChooser getFileChooser()
	{
		if(this.inputChooser == null)
		{
			this.inputChooser = new JFileChooser();
			this.inputChooser.setDialogTitle("Select input directory");
			this.inputChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		
		return this.inputChooser;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		if(arg0.getSource() == this.inputPathButton)
		{
			int returnVal = this.getFileChooser().showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				//System.out.println("Current: "+this.getFileChooser().getCurrentDirectory().getAbsolutePath());
				//System.out.println("Selected file: "+this.getFileChooser().getSelectedFile().getAbsolutePath());//Should user file value
				
				this.inputPathField.setText(this.getFileChooser().getSelectedFile().getAbsolutePath());
			}
		}
		if(arg0.getSource() == this.buttonExecSelection)
		{
			if(this.optimizer != null)
			{		
				try {
					this.optimizer.execPortfolioElemination(this.inputPathField.getText());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if(arg0.getSource() == this.buttonExecOpti)
		{
			if(this.optimizer != null)
			{
				int count = this.tableStats.getRowCount();
				ArrayList<StockEntry> list = new ArrayList<StockEntry>();
				StockEntry entry = null;
				boolean selected = false;
				for(int i=0;i<count;i++)
				{
					selected = (boolean)this.tableStats.getModel().getValueAt(i, StatsTableModel.COLUMN_SELECTION);
					if(selected)
					{
						entry = new StockEntry();
						entry.symbol = (String)this.tableStats.getModel().getValueAt(i, 0);
						list.add(entry);
					}
					
				}
				
				
				try {
					this.optimizer.execPortfolioOptimization(list);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void loadStatDataToTable(List<StockEntry> data)
	{
		this.efficientStocks = data;
		((StatsTableModel)this.tableStats.getModel()).reloadData(data);
	}
	
	public void loadOptiDataToTable(List<StockEntry> list, double[] esp,double[] var, ArrayList<double[]> proportions)
	{
		((OptiTableModel)this.tableOpti.getModel()).reloadData(list, esp,var, proportions);
	}
	
	public void displayProgress(int progress, String message)
	{
		this.progressBar.setValue(progress);
		this.progressBar.setString(progress+"%");
		this.labelProgress.setText(message);
	}
	
	class OptiTableModel extends DefaultTableModel
	{
		public Class getColumnClass(int column)
		{
			return double.class;
		}
		
		public void reloadData(List<StockEntry> list, double[] esp,double[] var, ArrayList<double[]> proportions)
		{
			int count = list.size();
			int levelCount = proportions.size();
			String [] headers = new String[count+2];
			String sym;
			String [] colHeaders = new String[levelCount+1];
			headers[0] = "Expected value";
			headers[1] = "Risque(Variance)";
			colHeaders[0] = " ";
			
			for(int i=0;i<count;i++)
			{
				sym = list.get(i).symbol;
				headers[i+2] = sym;
			}
			
			for(int i=0;i<levelCount;i++)
			{
				colHeaders[i+1] = new String("Level "+i);
			}
			
			this.setColumnCount(levelCount+1);
			this.setColumnIdentifiers(colHeaders);
			
			Object[] rowData;
			double [] rowProp;
			rowData = new Object[levelCount+1];//row for esp
			rowData[0] = headers[0];
			for(int i=0;i<levelCount;i++)
			{
				rowData[i+1] = esp[i];
			}
			this.addRow(rowData);
			
			rowData = new Object[levelCount+1];//row for var
			rowData[0] = headers[1];
			for(int i=0;i<levelCount;i++)
			{
				rowData[i+1] = var[i];
			}
			this.addRow(rowData);
			
			for(int i=0;i<count;i++)
			{
				rowData = new Object[levelCount+1];
				rowData[0] = headers[i+2];
				
				for(int j=0;j<levelCount;j++)
				{
					rowProp = proportions.get(j);
					rowData[j+1] = String.format("%.3f", rowProp[i]*100);
				}
				
				this.addRow(rowData);
			}
		}
	}
	
	class StatsTableModel extends DefaultTableModel
	{
		Object nullData[] = {"empty", "0","0",new Boolean(false)};
		public static final int COLUMN_SELECTION = 3;
		String [] tableCols = {"Symbol", "Expected profit(mean)","risk(variance)","Select"}; 
		
		public StatsTableModel()
		{
			this.setColumnIdentifiers(tableCols);
			this.addRow(nullData);
		}
		
		public Class getColumnClass(int column)
		{
			return nullData[column].getClass();
		}
		
		public void reloadData(List<StockEntry> data)
		{
			int count = this.getRowCount();
			for(int i=0;i<count;i++)
				this.removeRow(0);
			
			for(StockEntry entry : data)
			{
				String meanStr = String.format("%.8f", entry.mean);
				String varStr = String.format("%.8f", entry.var);
				this.addRow(new Object[]{entry.symbol, meanStr,varStr,new Boolean(false)});
			}
		}
	}
}
