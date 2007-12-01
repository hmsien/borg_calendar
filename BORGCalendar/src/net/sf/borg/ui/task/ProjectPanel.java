/*
 This file is part of BORG.

 BORG is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 BORG is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with BORG; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Copyright 2003 by Mike Berger
 */

package net.sf.borg.ui.task;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;

import net.sf.borg.common.Errmsg;
import net.sf.borg.common.Resource;
import net.sf.borg.common.Warning;
import net.sf.borg.model.CategoryModel;
import net.sf.borg.model.Model;
import net.sf.borg.model.TaskModel;
import net.sf.borg.model.beans.Project;
import net.sf.borg.model.beans.Task;
import net.sf.borg.model.db.DBException;
import net.sf.borg.ui.MultiView;
import net.sf.borg.ui.ResourceHelper;
import net.sf.borg.ui.RunReport;
import net.sf.borg.ui.util.PopupMenuHelper;
import net.sf.borg.ui.util.StripedTable;
import net.sf.borg.ui.util.TablePrinter;
import net.sf.borg.ui.util.TableSorter;

/**
 * 
 * @author MBERGER
 * @version
 */

// task tracker main window
// this view shows a list of tasks in a table format with all kinds
// of sorting/filtering options. It is really like the "main" window
// for a whole task traking application separate from the calendar
// application. In prior non-java versions of BORG, the task tracker
// and calendar apps were completely separate apps.
public class ProjectPanel extends JPanel implements Model.Listener {

    private class ProjIntRenderer extends JLabel implements TableCellRenderer {

	public ProjIntRenderer() {
	    super();
	    setOpaque(true); // MUST do this for background to show up.
	}

	public Component getTableCellRendererComponent(JTable table,
		Object obj, boolean isSelected, boolean hasFocus, int row,
		int column) {

	    JLabel l = (JLabel)defrend_.getTableCellRendererComponent(table, obj,
			isSelected, hasFocus, row, column);

	    String nm = table.getColumnName(column);
	    if( obj == null || !nm.equals(Resource.getPlainResourceString("Days_Left")))
		return l;
	    
	    int i = ((Integer) obj).intValue();

	    this.setText(l.getText());
	    this.setHorizontalAlignment(CENTER);
	    this.setBackground(l.getBackground());
	    this.setForeground(l.getForeground());


	    if (i == 9999)
		this.setText("--");
	    
	    if( isSelected )
		return this;

	    // yellow alert -- <10 days left
	    if (i < 10)
		this.setBackground(new Color(255, 255, 175));

	    if (i < 5)
		this.setBackground(new Color(255, 200, 120));

	    // red alert -- <2 days left
	    if (i < 2) {
		this.setBackground(new Color(255, 120, 120));
	    }

	    return this;
	}
    }

    private JMenuItem add = new JMenuItem();

    private JButton addbutton = null;

    private JPanel buttonPanel = null;

    private JMenuItem change = new JMenuItem();

    private JButton changebutton1 = null;

    private JMenuItem clone = new JMenuItem();

    private JButton clonebutton1 = null;

    private JMenuItem close = new JMenuItem();

    private JButton closebutton1 = null;

    private TableCellRenderer defrend_;

    private JMenuItem delete = new JMenuItem();

    private JButton deletebutton1 = null;

    private StripedTable projectTable;

    private JComboBox pstatusBox = new JComboBox();

    /** Creates new form btgui */
    public ProjectPanel() {
	super();
	TaskModel.getReference().addListener(this);
	try {
	    initComponents();
	    projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    refresh();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    Errmsg.errmsg(e);
	    return;
	}

    }

    public void print() {

	// print the current table of tasks
	try {
	    TablePrinter.printTable(projectTable);
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}
    }

    // refresh is called to update the table of shown tasks due to model
    // changes
    // or if the user
    // changes the filtering criteria
    public void refresh() {

	int row = 0;

	// clear all table rows

	deleteAllProjects();

	String pstatfilt = (String) pstatusBox.getSelectedItem();

	try {
	    TaskModel taskmod_ = TaskModel.getReference();

	    // add projects to project table
	    Collection projects = taskmod_.getProjects();
	    Iterator ti = projects.iterator();
	    while (ti.hasNext()) {

		Project p = (Project) ti.next();

		if (!pstatfilt.equals(Resource.getPlainResourceString("All"))
			&& !pstatfilt.equals(p.getStatus()))
		    continue;

		// category
		String cat = p.getCategory();
		if (cat == null || cat.equals(""))
		    cat = CategoryModel.UNCATEGORIZED;

		if (!CategoryModel.getReference().isShown(cat))
		    continue;

		// if we get here - we are displaying this task as a row
		// so fill in an array of objects for the row
		Object[] ro = new Object[10];
		ro[0] = p.getId();
		ro[1] = p.getCategory();
		ro[2] = p.getStatus();
		ro[3] = p.getStartDate();
		ro[4] = p.getDueDate();
		Collection ptasks = TaskModel.getReference().getTasks(
			p.getId().intValue());
		ro[5] = new Integer(ptasks.size());
		int open = 0;
		Iterator it = ptasks.iterator();
		while (it.hasNext()) {
		    Task pt = (Task) it.next();
		    if (!TaskModel.isClosed(pt)) {
			open++;
		    }
		}
		ro[6] = new Integer(open);
		;
		ro[7] = new Integer(0);

		// calculate days left - today - duedate
		if (ro[4] == null)
		    // 9999 days left if no due date - this is a (cringe,
		    // ack,
		    // thptt) magic value
		    ro[7] = new Integer(9999);
		else {
		    Date dd = (Date) ro[4];
		    ro[7] = new Integer(TaskModel.daysLeft(dd));
		}

		// strip newlines from the description
		String de = p.getDescription();
		String tmp = "";
		for (int i = 0; de != null && i < de.length(); i++) {
		    char c = de.charAt(i);
		    if (c == '\n' || c == '\r') {
			tmp += ' ';
			continue;
		    }

		    tmp += c;
		}
		ro[8] = tmp;

		// add the task row to table
		addRow(projectTable, ro);
		row++;
	    }

	} catch (DBException e) {
	    if (e.getRetCode() != DBException.RET_NOT_FOUND) {
		Errmsg.errmsg(e);
	    }

	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }

    public void showTasksForProject(Project p) {
	MultiView.getMainView().showTasksForProject(p);
    }

    private void addActionPerformed(java.awt.event.ActionEvent evt) {
	// ask controller to bring up new task editor

	project_add();

    }

    // add a row to the sorted table
    private void addRow(JTable t, Object[] ro) {
	TableSorter tm = (TableSorter) t.getModel();
	tm.addRow(ro);
	tm.tableChanged(new TableModelEvent(tm));
    }

    private void changeActionPerformed(java.awt.event.ActionEvent evt) {

	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);

	// ask borg class to bring up a task editor window
	project_change(num.intValue());

    }

    private void cloneActionPerformed(java.awt.event.ActionEvent evt) {

	// get task number from column 0 of selected row
	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);

	// ask borg class to bring up a task editor window
	project_clone(num.intValue());

    }

    private void closeActionPerformed(java.awt.event.ActionEvent evt) {

	// get the task number from column 0 of the selected row
	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);
	try {
	    // force close of the task
	    TaskModel taskmod_ = TaskModel.getReference();
	    taskmod_.closeProject(num.intValue());
	} catch (Warning w) {
	    Errmsg.notice(w.getMessage());
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }
    
    private void ganttActionPerformed(java.awt.event.ActionEvent evt) {

	// get the task number from column 0 of the selected row
	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);
	try {
	    // force close of the task
	    TaskModel taskmod_ = TaskModel.getReference();
	    Project p = taskmod_.getProject(num.intValue());
	    GanttFrame.showChart(p);
	} catch (ClassNotFoundException cnf)
	{
	    Errmsg.notice(Resource.getPlainResourceString("borg_jasp"));   
	} catch (NoClassDefFoundError r) {
	    Errmsg.notice(Resource.getPlainResourceString("borg_jasp"));
	} catch (Warning w) {
	    Errmsg.notice(w.getMessage());
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }

    private void deleteActionPerformed(java.awt.event.ActionEvent evt) {

	// delete selected row

	// get task number from column 0 of the selected row
	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);

	// prompt for ok
	int ret = JOptionPane.showConfirmDialog(null, Resource
		.getResourceString("Really_delete_number_")
		+ num, "", JOptionPane.YES_NO_OPTION);
	if (ret == JOptionPane.YES_OPTION) {
	    // delete the task
	    try {
		TaskModel taskmod_ = TaskModel.getReference();
		taskmod_.deleteProject(num.intValue());
	    } catch (Exception e) {
		Errmsg.errmsg(e);
	    }
	}

    }

    private void deleteAllProjects() {
	TableSorter tm = (TableSorter) projectTable.getModel();
	tm.setRowCount(0);
	tm.tableChanged(new TableModelEvent(tm));
    }

    /**
     * This method initializes addbutton	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getAddbutton() {
        if (addbutton == null) {
    	addbutton = new JButton();
    	addbutton.setText(Resource.getPlainResourceString("Add"));
    	addbutton.setIcon(new ImageIcon(getClass().getResource("/resource/Add16.gif")));
    	addbutton.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		addActionPerformed(e);
    	    }
    	});
        }
        return addbutton;
    }
    
    private JButton ganttbutton;
    private JButton getGanttbutton() {
        if (ganttbutton == null) {
            ganttbutton = new JButton();
            ganttbutton.setText(Resource.getPlainResourceString("GANTT"));
            //ganttbutton.setIcon(new ImageIcon(getClass().getResource("/resource/Add16.gif")));
            ganttbutton.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		ganttActionPerformed(e);
    	    }
    	});
        }
        return ganttbutton;
    }
    
    private ActionListener getAL(JMenuItem mnuitm) {
	return mnuitm.getActionListeners()[0];
    }

    /**
     * This method initializes buttonPanel	
     * 	
     * @return javax.swing.JPanel	
     */
    private JPanel getButtonPanel() {
        if (buttonPanel == null) {
    	buttonPanel = new JPanel();
    	buttonPanel.setLayout(new FlowLayout());
    	buttonPanel.add(getAddbutton(), null);
    	buttonPanel.add(getChangebutton1(), null);
    	buttonPanel.add(getDeletebutton1(), null);
    	buttonPanel.add(getClosebutton1(), null);
    	buttonPanel.add(getClonebutton1(), null);
    	buttonPanel.add(getGanttbutton(), null);
    	
    	JButton projRptButton = new JButton();
	ResourceHelper.setText(projRptButton, "Report");
	projRptButton.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		reportButtonActionPerformed(evt);
	    }
	});
	buttonPanel.add(projRptButton);
        }
        return buttonPanel;
    }

    /**
     * This method initializes changebutton1	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getChangebutton1() {
        if (changebutton1 == null) {
    	changebutton1 = new JButton();
    	changebutton1.setIcon(new ImageIcon(getClass().getResource("/resource/Edit16.gif")));
    	changebutton1.setText(Resource.getPlainResourceString("Change"));
    	changebutton1.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		changeActionPerformed(e);
    	    }
    	});
        }
        return changebutton1;
    }

    /**
     * This method initializes clonebutton1	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getClonebutton1() {
        if (clonebutton1 == null) {
    	clonebutton1 = new JButton();
    	clonebutton1.setIcon(new ImageIcon(getClass().getResource("/resource/Copy16.gif")));
    	clonebutton1.setText(Resource.getPlainResourceString("Clone"));
    	clonebutton1.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		cloneActionPerformed(e);
    	    }
    	});
        }
        return clonebutton1;
    }

    /**
     * This method initializes closebutton1	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getClosebutton1() {
        if (closebutton1 == null) {
    	closebutton1 = new JButton();
    	closebutton1.setIcon(new ImageIcon(getClass().getResource("/resource/greenlight.gif")));
    	closebutton1.setText(Resource.getPlainResourceString("Close"));
    	closebutton1.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		closeActionPerformed(e);
    	    }
    	});
        }
        return closebutton1;
    }

    /**
     * This method initializes deletebutton1	
     * 	
     * @return javax.swing.JButton	
     */
    private JButton getDeletebutton1() {
        if (deletebutton1 == null) {
    	deletebutton1 = new JButton();
    	deletebutton1.setIcon(new ImageIcon(getClass().getResource("/resource/Delete16.gif")));
    	deletebutton1.setText(Resource.getPlainResourceString("Delete"));
    	deletebutton1.addActionListener(new java.awt.event.ActionListener() {
    	    public void actionPerformed(java.awt.event.ActionEvent e) {
    		deleteActionPerformed(e);
    	    }
    	});
        }
        return deletebutton1;
    }
    
    /**
         * This method is called from within the constructor to initialize the
         * form. WARNING: Do NOT modify this code. The content of this method is
         * always regenerated by the FormEditor.
         */

    private void initComponents() throws Exception {

	GridBagConstraints gridBagConstraints = new GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 2;
	this.setLayout(new java.awt.GridBagLayout());
	initMenuBar();
	FlowLayout flowLayout = new FlowLayout();
	flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
	JLabel statusLabel = new JLabel();
	statusLabel.setText(Resource.getPlainResourceString("Status") + ":");
	JPanel pnl = new JPanel();
	pnl.setLayout(flowLayout);
	pnl.add(statusLabel, null);

	pstatusBox.removeAllItems();
	pstatusBox.addItem(Resource.getPlainResourceString("All"));
	pstatusBox.addItem(Resource.getPlainResourceString("OPEN"));
	pstatusBox.addItem(Resource.getPlainResourceString("CLOSED"));
	pstatusBox.setSelectedItem(Resource.getPlainResourceString("OPEN"));
	pstatusBox.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		refresh();
	    }
	});

	pnl.add(pstatusBox, null);
	

	GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
	gridBagConstraints1.gridx = 0;
	gridBagConstraints1.gridy = 0;
	gridBagConstraints1.weightx = 0.0;
	gridBagConstraints1.weighty = 0.0;
	gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints1.gridwidth = 1;
	gridBagConstraints1.insets = new java.awt.Insets(4, 4, 4, 4);
	this.add(pnl, gridBagConstraints1);

	JScrollPane jScrollPane1 = new JScrollPane();
	projectTable = new StripedTable();

	GridBagConstraints gridBagConstraints11 = new GridBagConstraints();

	gridBagConstraints11.gridx = 0;
	gridBagConstraints11.gridy = 1;
	gridBagConstraints11.weightx = 1.0;
	gridBagConstraints11.weighty = 1.0;
	gridBagConstraints11.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints11.gridwidth = 1;
	gridBagConstraints11.insets = new java.awt.Insets(4, 4, 4, 4);
	this.add(jScrollPane1, gridBagConstraints11);

	this.add(getButtonPanel(), gridBagConstraints);
	// add scroll to the table
	jScrollPane1.setViewportView(projectTable);

	defrend_ = projectTable.getDefaultRenderer(Integer.class);
	// set renderer to the custom one for integers
	projectTable.setDefaultRenderer(Integer.class,
		new ProjectPanel.ProjIntRenderer());

	// use a sorted table model
	projectTable.setModel(new TableSorter(new String[] {
		Resource.getPlainResourceString("Item_#"),
		Resource.getPlainResourceString("Category"),
		Resource.getPlainResourceString("Status"),
		Resource.getPlainResourceString("Start_Date"),
		Resource.getPlainResourceString("Due_Date"),
		Resource.getPlainResourceString("total_tasks"),
		Resource.getPlainResourceString("open_tasks"),
		Resource.getPlainResourceString("Days_Left"),
		Resource.getPlainResourceString("Description") }, new Class[] {
		java.lang.Integer.class, java.lang.String.class, String.class,
		Date.class, Date.class, java.lang.Integer.class, Integer.class,
		Integer.class, java.lang.String.class }));

	new PopupMenuHelper(projectTable, new PopupMenuHelper.Entry[] {
		new PopupMenuHelper.Entry(getAL(add), "Add"),
		new PopupMenuHelper.Entry(getAL(change), "Change"),
		new PopupMenuHelper.Entry(getAL(clone), "Clone"),
		new PopupMenuHelper.Entry(getAL(delete), "Delete"),
		new PopupMenuHelper.Entry(getAL(close), "Close"),
		new PopupMenuHelper.Entry(getAL(ganttmi), "GANTT")});

	projectTable.getColumnModel().getColumn(0).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(1).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(2).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(3).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(5).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(6).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(7).setPreferredWidth(80);
	projectTable.getColumnModel().getColumn(8).setPreferredWidth(400);

	// set up for sorting when a column header is clicked
	TableSorter tm = (TableSorter) projectTable.getModel();
	tm.addMouseListenerToHeaderInTable(projectTable);

	// clear all rows
	deleteAllProjects();

	// jScrollPane1.setViewport(jScrollPane1.getViewport());
	jScrollPane1.setViewportView(projectTable);
	jScrollPane1.setBorder(javax.swing.BorderFactory
		.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
	projectTable.setBorder(new javax.swing.border.LineBorder(
		new java.awt.Color(0, 0, 0)));
	projectTable
		.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
	//projectTable.setGridColor(java.awt.Color.blue);
	// projectTable.setPreferredSize(new java.awt.Dimension(700,
	// 500));
	projectTable.addMouseListener(new java.awt.event.MouseAdapter() {
	    public void mouseClicked(java.awt.event.MouseEvent evt) {
		mouseClick(evt);
	    }
	});

	

    }

    private void initMenuBar() {

	JMenuBar menuBar = new JMenuBar();

	JMenu editMenu = new JMenu();

	ResourceHelper.setText(editMenu, "Action");
	ResourceHelper.setText(add, "Add");
	add.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		addActionPerformed(evt);
	    }
	});

	editMenu.add(add);

	ResourceHelper.setText(change, "Change");
	change.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		changeActionPerformed(evt);
	    }
	});

	editMenu.add(change);

	ResourceHelper.setText(clone, "Clone");
	clone.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		cloneActionPerformed(evt);
	    }
	});

	editMenu.add(clone);

	ResourceHelper.setText(delete, "Delete");
	delete.setName("delete");
	delete.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		deleteActionPerformed(evt);
	    }
	});

	editMenu.add(delete);

	ResourceHelper.setText(close, "Close");
	close.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		closeActionPerformed(evt);
	    }
	});

	editMenu.add(close);
	
	
	ResourceHelper.setText(ganttmi, "GANTT");
	ganttmi.addActionListener(new java.awt.event.ActionListener() {
	    public void actionPerformed(java.awt.event.ActionEvent evt) {
		ganttActionPerformed(evt);
	    }
	});

	editMenu.add(ganttmi);

	menuBar.add(editMenu);

    }
    private JMenuItem ganttmi = new JMenuItem();
    private void mouseClick(java.awt.event.MouseEvent evt) {

	// ask controller to bring up task editor on double click
	if (evt.getClickCount() < 2)
	    return;

	// changeActionPerformed(null);
	showChildren();
    }

    private void project_add() {
	try {
	    // display the task editor
	    MultiView.getMainView().addView(new ProjectView(null, ProjectView.T_ADD, null));
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}
    }

    private void project_change(int id) {

	try {
	    // get the task from the data model
	    TaskModel taskmod_ = TaskModel.getReference();
	    Project p = taskmod_.getProject(id);
	    if (p == null)
		return;

	    // display the task editor
	    MultiView.getMainView().addView(new ProjectView(p, ProjectView.T_CHANGE, null));

	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }

    private void project_clone(int id) {

	try {
	    // get the task
	    TaskModel taskmod_ = TaskModel.getReference();
	    Project p = taskmod_.getProject(id);
	    if (p == null)
		return;

	    // display the task editor
	    MultiView.getMainView().addView(new ProjectView(p, ProjectView.T_CLONE, null));
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }
    
    private void showChildren() {

	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer num = (Integer) tm.getValueAt(row, 0);

	try {
	    Project p = TaskModel.getReference().getProject(num.intValue());
	    showTasksForProject(p);
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }
    
    private void reportButtonActionPerformed(java.awt.event.ActionEvent evt) {

	// get the task number from column 0 of the selected row
	int row = projectTable.getSelectedRow();
	if (row == -1)
	    return;
	TableSorter tm = (TableSorter) projectTable.getModel();
	Integer pnum = (Integer) tm.getValueAt(row, 0);
	try {
	    Map map = new HashMap();
	    map.put("pid", pnum);
	    RunReport.runReport("proj", map );
	} catch (NoClassDefFoundError r) {
	    Errmsg.notice(Resource.getPlainResourceString("borg_jasp"));
	} catch (Exception e) {
	    Errmsg.errmsg(e);
	}

    }

    public void remove() {
	// TODO Auto-generated method stub
	
    }

}  //  @jve:decl-index=0:visual-constraint="-18,21"
