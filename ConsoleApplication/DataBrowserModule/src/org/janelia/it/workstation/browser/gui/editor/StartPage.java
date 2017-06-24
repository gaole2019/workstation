package org.janelia.it.workstation.browser.gui.editor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.janelia.it.jacs.model.domain.report.DatabaseSummary;
import org.janelia.it.jacs.model.domain.report.DiskUsageSummary;
import org.janelia.it.jacs.model.domain.report.QuotaUsage;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.gui.listview.ViewerToolbar;
import org.janelia.it.workstation.browser.gui.options.ApplicationOptions;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SelectablePanel;
import org.janelia.it.workstation.browser.nb_action.NewFilterActionListener;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.sql.DataSet;
import net.miginfocom.swing.MigLayout;

/**
 * Start Page which is automatically shown to the user on every startup (unless disabled by user preference) 
 * and allows for easy navigation to common tasks and use cases.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class StartPage extends JPanel implements PropertyChangeListener {

    private static final Logger log = LoggerFactory.getLogger(StartPage.class);
    
    private static final ImageIcon DISK_USAGE_ICON = Icons.getIcon("database_400.png");
    private static final ImageIcon SAMPLE_ICON = Icons.getIcon("microscope_400.png");
    
    private static final int iconSize = 150;
    private static final String SUMMARY_MINE = " My data ";
    private static final String SUMMARY_ALL = " All data ";
    
    private final JPanel topPanel;
    private final JPanel mainPanel;
    private final JPanel searchPanel;
    private final JPanel diskSpacePanel;
    private final JPanel dataSummaryPanel;
    private final JPanel lowerPanel = new JPanel();
    private final JCheckBox openOnStartupCheckbox = new JCheckBox("Show On Startup");
    private final JTextField searchField;
    private final JButton searchButton;

    private Font titleFont;
    private Font largeFont;
    private Font mediumFont;
    
    private Class<?> searchClass;
    private JLabel spaceUsedLabel;
    private JLabel labSpaceUsedLabel;
    private JLabel spaceAvailableLabel;
    private JLabel dataSetCountLabel;
    private JLabel sampleCountLabel;
    private JLabel lsmCountLabel;
    private JLabel annotationCountLabel;

    private DiskUsageSummary diskUsageSummary;
    private DatabaseSummary dataSummary;
    private ImageIcon diskUsageIcon;
    private ImageIcon sampleIcon;
    
    public StartPage() {
        
        diskUsageIcon = getScaledIcon(DISK_USAGE_ICON, iconSize, iconSize);
        sampleIcon = getScaledIcon(SAMPLE_ICON, iconSize, iconSize);
        
        JLabel titleLabel = new JLabel("Welcome to the Janelia Workstation");
        titleLabel.setForeground(UIManager.getColor("textInactiveText"));
        
        titleFont = titleLabel.getFont().deriveFont(Font.BOLD, 20);
        largeFont = titleLabel.getFont().deriveFont(Font.BOLD, 16);
        mediumFont = titleLabel.getFont().deriveFont(Font.PLAIN, 16);
        
        // Top Panel
        
        titleLabel.setFont(titleFont);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        
        topPanel = new ViewerToolbar() {
            @Override
            protected void refresh() {
                StartPage.this.refresh();
            }
        };
        
        // Search Panel
        
        searchField = new JTextField();
        searchField.setColumns(30);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchButton.doClick();
                }
            }
        });
        
        searchButton = new JButton("Search");
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NewFilterActionListener actionListener = new NewFilterActionListener(searchField.getText(), searchClass);
                actionListener.actionPerformed(e);
            }
        });

        ButtonGroup group = new ButtonGroup();
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

//        JToggleButton button0 = new JToggleButton("Everything");
//        button0.setSelected(true);
//        button0.setMargin(new Insets(5,5,5,5));
//        button0.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                // TODO: this needs to search everything
//                searchClass = Sample.class.getSimpleName();
//            }
//        });
//        group.add(button0);
//        buttonsPanel.add(button0);
                
        JToggleButton button1 = new JToggleButton("Confocal Samples");
        button1.setMargin(new Insets(5,5,5,5));
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = Sample.class;
            }
        });
        group.add(button1);
        buttonsPanel.add(button1);

        JToggleButton button2 = new JToggleButton("LSM Images");
        button2.setMargin(new Insets(5,5,5,5));
        button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = LSMImage.class;
            }
        });
        group.add(button2);
        buttonsPanel.add(button2);

        JToggleButton button3 = new JToggleButton("Mouse Samples");
        button3.setMargin(new Insets(5,5,5,5));
        button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchClass = TmSample.class;
            }
        });
        group.add(button3);
        buttonsPanel.add(button3);

        // Default search button
        button1.setSelected(true);
        searchClass = Sample.class;
        
        JLabel promptLabel = new JLabel("What would you like to search?");
        promptLabel.setFont(largeFont);
        
        searchPanel = new JPanel();
        searchPanel.setLayout(new MigLayout("gap 50, fill, wrap 2", "[grow 50]5[grow 50]", "[grow 50]5[grow 0]5[grow 75]"));
        searchPanel.add(titleLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(promptLabel, "gap 10, span 2, al center bottom");
        searchPanel.add(buttonsPanel, "span 2, al center");
        searchPanel.add(searchField, "height 35, al right top");
        searchPanel.add(searchButton, "al left top");

        // Disk Space Panel

        diskSpacePanel = new SelectablePanel();
        diskSpacePanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]"));
        
        // Data Summary Panel
        dataSummaryPanel = new SelectablePanel();
        dataSummaryPanel.setLayout(new MigLayout("gap 50, fillx, wrap 3", "[grow 10]5[grow 0]5[grow 10]", "[]2[]5[]5[]5[]5[]"));
        
        
        // Main Panel
        
        mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout("gap 50, fillx, wrap 2", "[]5[]", "[]10[]"));
        mainPanel.add(searchPanel, "span 2, al center top");
        mainPanel.add(diskSpacePanel, "al center top, width 50%");
        mainPanel.add(dataSummaryPanel, "al center top, width 50%");
        
        // Lower panel
        
        openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        openOnStartupCheckbox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ApplicationOptions.getInstance().setShowStartPageOnStartup(openOnStartupCheckbox.isSelected());
            }
        });
        
        lowerPanel.setLayout(new BorderLayout());
        lowerPanel.add(openOnStartupCheckbox);
        
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(lowerPanel, BorderLayout.SOUTH);
        
        refresh();
    }
    
    private void refresh() {
        
        log.info("Refreshing start page");

        diskSpacePanel.removeAll();
        diskSpacePanel.add(getLargeLabel("Disk Space Usage"), "spanx 3, gapbottom 10, al center");
        diskSpacePanel.add(new JLabel(diskUsageIcon), "al right top, w 50%");
        diskSpacePanel.add(new JLabel(Icons.getLoadingIcon()), "spanx 2, al center center");

        dataSummaryPanel.removeAll();
        dataSummaryPanel.add(getLargeLabel("Confocal Data Summary"), "spanx 3, gapbottom 10, al center");
        dataSummaryPanel.add(new JLabel(sampleIcon), "al right top, w 50%");
        dataSummaryPanel.add(new JLabel(Icons.getLoadingIcon()), "spanx 2, al center center");
        
        mainPanel.updateUI();
        
        SimpleWorker worker = new SimpleWorker() {

            private DiskUsageSummary summary;
            
            @Override
            protected void doStuff() throws Exception {
                summary = DomainMgr.getDomainMgr().getDomainFacade().getDiskUsageSummary();   
            }

            @Override
            protected void hadSuccess() {
                diskUsageSummary = summary;
                populateDiskView(diskUsageSummary);
            }

            @Override
            protected void hadError(Throwable e) {
                ConsoleApp.handleException(e);
                diskUsageSummary = null;
                populateDiskView(diskUsageSummary);
            }
        };

        worker.execute();
        
        SimpleWorker worker2 = new SimpleWorker() {

            private DatabaseSummary summary;
            
            @Override
            protected void doStuff() throws Exception {
                summary = DomainMgr.getDomainMgr().getDomainFacade().getDatabaseSummary();
            }

            @Override
            protected void hadSuccess() {
                dataSummary = summary;
                populateDataView(dataSummary);
            }

            @Override
            protected void hadError(Throwable e) {
                ConsoleApp.handleException(e);
                dataSummary = null;
                populateDataView(dataSummary);
            }
        };

        worker2.execute();
    }

    private void populateDiskView(DiskUsageSummary dataSummary) {
        
        // Reset components
        diskSpacePanel.removeAll();
        
        spaceUsedLabel = getMediumLabel("");
        labSpaceUsedLabel = getMediumLabel("");
        spaceAvailableLabel = getMediumLabel("");
        
        diskSpacePanel.add(getLargeLabel("Disk Space Usage"), "spanx 3, gapbottom 10, al center");
        diskSpacePanel.add(new JLabel(diskUsageIcon), "spany 3, al right top");
        
        diskSpacePanel.add(getMediumLabel("Your usage:"), "al left top, width 30pt");
        diskSpacePanel.add(spaceUsedLabel, "al left top, width 30pt");
        
        diskSpacePanel.add(getMediumLabel("Lab's usage:"), "al left top");
        diskSpacePanel.add(labSpaceUsedLabel, "al left top");
        
        diskSpacePanel.add(getMediumLabel("Free space:"), "al left top");
        diskSpacePanel.add(spaceAvailableLabel, "al left top");
        
        if (dataSummary==null) return;
        
        Double userDataSetsTB = dataSummary.getUserDataSetsTB();
        if (userDataSetsTB!=null) {
            spaceUsedLabel.setText(String.format("%2.2f TB", userDataSetsTB));
        }
        
        QuotaUsage quotaUsage = dataSummary.getQuotaUsage();
        if (quotaUsage!=null) {
            Double spaceUsedTB = quotaUsage.getSpaceUsedTB();
            Double totalSpaceTB = quotaUsage.getTotalSpaceTB();
            if (spaceUsedTB!=null) {
                labSpaceUsedLabel.setText(String.format("%2.2f TB", spaceUsedTB));
                if (totalSpaceTB!=null) {
                    double spaceAvailable = totalSpaceTB - spaceUsedTB;
                    spaceAvailableLabel.setText(String.format("%2.2f TB", spaceAvailable));
                }
            }
        }
        
    }
    
    private void populateDataView(DatabaseSummary dataSummary) {

        // Reset components
        dataSummaryPanel.removeAll();

        dataSetCountLabel = getMediumLabel("");
        sampleCountLabel = getMediumLabel("");
        lsmCountLabel = getMediumLabel("");
        annotationCountLabel = getMediumLabel("");

        dataSummaryPanel.add(getLargeLabel("Confocal Data Summary"), "spanx 3, gapbottom 10, al center");
        dataSummaryPanel.add(new JLabel(sampleIcon), "spany 5, al right top");
        
        dataSummaryPanel.add(getMediumLabel("Data Sets:"), "al left top");
        dataSummaryPanel.add(dataSetCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Samples:"), "al left top");
        dataSummaryPanel.add(sampleCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("LSM Images:"), "al left top");
        dataSummaryPanel.add(lsmCountLabel, "al left top");
        
        dataSummaryPanel.add(getMediumLabel("Annotations:"), "al left top");
        dataSummaryPanel.add(annotationCountLabel, "al left top");
        
        if (dataSummary==null) return;

        Map<String, Long> counts = dataSummary.getUserCounts(); 

        if (counts!=null) {
            dataSetCountLabel.setText(counts.get(DataSet.class.getSimpleName())+"");
            sampleCountLabel.setText(counts.get(Sample.class.getSimpleName())+"");
            lsmCountLabel.setText(counts.get(LSMImage.class.getSimpleName())+"");
            annotationCountLabel.setText(counts.get(Annotation.class.getSimpleName())+"");
        }
    }
    
    private ImageIcon getScaledIcon(ImageIcon icon, int width, int height) {
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(width, height,  java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(newimg);
    }
    
    private JLabel getLargeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(largeFont);
        return label;
    }

    private JLabel getMediumLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(mediumFont);
        return label;
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ApplicationOptions.PROP_SHOW_START_PAGE_ON_STARTUP)) {
            openOnStartupCheckbox.setSelected(ApplicationOptions.getInstance().isShowStartPageOnStartup());
        }
    }
}
