package org.janelia.it.workstation.gui.large_volume_viewer.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for editing the name for a Workspace, based on a pre-determined naming pattern.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EditWorkspaceNameDialog extends ModalDialog {

    private static final String DEFAULT_NAME = "new workspace";
    
    private final JPanel attrPanel;
    private JTextField sampleDateField;
    private JTextField neuronCodeField;
    private JTextField userInitialsField;
    private JTextField suffixField;
    private JCheckBox overrideCheckbox;
    private JTextField nameField;
    private boolean save = false;
    private String name;
    
    public EditWorkspaceNameDialog() {

        setTitle("Workspace Name");

        attrPanel = new JPanel(new MigLayout(
                "wrap 4, ins 10, fill"
        ));
        
        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });
        
        getRootPane().setDefaultButton(okButton);
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }
    
    public String showForSample(TmSample sample) {

        attrPanel.add(new JLabel("Sample Name: "+sample.getName()), "span 4");

        attrPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "span 4, gaptop 10lp, gapbottom 5lp, grow");
        
        attrPanel.add(new JLabel("Sample Date"), "");
        attrPanel.add(new JLabel("Neuron Code"), "");
        attrPanel.add(new JLabel("User Initials"), "");
        attrPanel.add(new JLabel("Suffix (optional)"), "");
        
        sampleDateField = new JTextField();
        attrPanel.add(sampleDateField,"width 50:100:1000, grow");
        sampleDateField.setText(guessSampleDate(sample.getName()));

        neuronCodeField = new JTextField();
        attrPanel.add(neuronCodeField,"width 100:200:1000, grow");
        
        userInitialsField = new JTextField();
        userInitialsField.setText(guessUserInitials(AccessManager.getAccessManager().getSubject().getFullName()));
        attrPanel.add(userInitialsField,"width 30:100:1000, grow");

        suffixField = new JTextField();
        attrPanel.add(suffixField,"width 100:200:1000, grow");

        attrPanel.add(new JSeparator(SwingConstants.HORIZONTAL), "span 4, gaptop 5lp, gapbottom 10lp, grow");
        
        overrideCheckbox = new JCheckBox("Manual override");
        overrideCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sampleDateField.setEnabled(!overrideCheckbox.isSelected());
                neuronCodeField.setEnabled(!overrideCheckbox.isSelected());
                userInitialsField.setEnabled(!overrideCheckbox.isSelected());
                suffixField.setEnabled(!overrideCheckbox.isSelected());
                nameField.setEnabled(overrideCheckbox.isSelected());
            }
        });       
        
        attrPanel.add(overrideCheckbox,"span 4");
        
        nameField = new JTextField();
        nameField.setText(DEFAULT_NAME);
        nameField.setEnabled(false);
        attrPanel.add(nameField,"span 4, grow");
        
        ActivityLogHelper.logUserAction("EditWorkspaceNameDialog.showForSample");
        packAndShow();
        return save?name:null;
    }
    
    public static String guessSampleDate(String sampleName) {

        Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}).*");
        Matcher m = p.matcher(sampleName);
        if (m.matches()) {
            return m.group(1);
        }
        
        return "";
    }
    
    public static String guessUserInitials(String fullName) {
        if (StringUtils.isEmpty(fullName)) return "";
        Pattern p = Pattern.compile("((^| )[A-Za-z])");
        Matcher m = p.matcher(fullName);
        String inititals = "";
        while (m.find()) {
            inititals+=m.group().trim();
        }
        return inititals.toUpperCase();
    }

    private void saveAndClose() {

        if (overrideCheckbox.isSelected()) {
            this.name = nameField.getText();
        }
        else {
            String sampleDate = sampleDateField.getText().trim();
            String neuronCode = neuronCodeField.getText().trim();
            String userInitials = userInitialsField.getText().trim();
            String suffix = suffixField.getText().trim();
            
            AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        
            if (StringUtils.isEmpty(sampleDate)) {
                annotationMgr.presentError("Sample date cannot be empty", "Missing input data");
            }
            
            if (StringUtils.isEmpty(neuronCode)) {
                annotationMgr.presentError("Neuron code cannot be empty", "Missing input data");
            }
            
            if (StringUtils.isEmpty(userInitials)) {
                annotationMgr.presentError("User initials cannot be empty", "Missing input data");
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append(sampleDate).append("-").append(neuronCode).append("-").append(userInitials);
            if (!StringUtils.isEmpty(suffix)) {
                sb.append("-").append(suffix);
            }
    
            this.name = sb.toString();
        }
        
        if (!StringUtils.isEmpty(name)) {
            this.save = true;
        }
        
        setVisible(false);
    }
}
