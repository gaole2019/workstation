package org.janelia.workstation.browser.gui.dialogs;

import static org.janelia.workstation.browser.gui.editor.FilterEditorPanel.DEFAULT_SEARCH_CLASS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;

import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.browser.gui.listview.icongrid.IconGridViewerConfiguration;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.TemplateEditorTextbox;
import org.janelia.workstation.common.gui.support.buttons.DropDownButton;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainObjectAttribute;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;

import net.miginfocom.swing.MigLayout;

/**
 * A dialog for configuring a IconGridViewer.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconGridViewerConfigDialog extends ModalDialog {

    private static final String DEFAULT_TITLE_VALUE = "{Name}";
    private static final String DEFAULT_SUBTITLE_VALUE = "";

    public static final int ERROR_OPTION = -1;
    public static final int CANCEL_OPTION = 0;
    public static final int CHOOSE_OPTION = 1;

    private int returnValue = ERROR_OPTION;

    private final DropDownButton typeCriteriaButton;
    private final JPanel attrPanel;
    private final TemplateEditorTextbox titleInputBox;
    private final TemplateEditorTextbox subtitleInputBox;

    private final IconGridViewerConfiguration config;

    private Class<? extends DomainObject> resultClass;

    private static final String PATTERN_HELP =
            "<html><font color='#959595' size='-1'>These templates are used to create the title and subtitle for each item. " +
            "You can use Ctrl-Space to show property suggestions. Properties must be surrounded in braces. " +
            "You can also use the | character to specify default values, for example: {Qi Score|\"None\"}"
                    + "</font></html>";

    public IconGridViewerConfigDialog(Class<? extends DomainObject> defaultResultClass) {

        this.config = IconGridViewerConfiguration.loadConfig();

        setTitle("Image View Configuration");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20", "[grow 0, growprio 0][grow 100, growprio 100]"));

        this.typeCriteriaButton = new DropDownButton();
        attrPanel.add(typeCriteriaButton,"gap para, span 2");

        attrPanel.add(new JLabel(PATTERN_HELP),"gap para, span 2, width 100:400:600, height 50:100:150, growx, ay top");

        titleInputBox = new TemplateEditorTextbox();
        subtitleInputBox = new TemplateEditorTextbox();
        JLabel titleLabel = new JLabel("Title format: ");
        titleLabel.setLabelFor(titleInputBox);
        attrPanel.add(titleLabel,"gap para");
        attrPanel.add(titleInputBox,"gap para, width 100:400:600, growx");

        JLabel subtitleLabel = new JLabel("Subtitle format: ");
        subtitleLabel.setLabelFor(subtitleInputBox);
        attrPanel.add(subtitleLabel,"gap para");
        attrPanel.add(subtitleInputBox,"gap para, width 100:400:600, growx");

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                returnValue = CANCEL_OPTION;
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

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                returnValue = CANCEL_OPTION;
            }
        });
        
        // Initialize the drop-down menu
        
        ButtonGroup typeGroup = new ButtonGroup();
        for (final Class<? extends DomainObject> searchClass : DomainUtils.getSearchClasses()) {
            final String type = DomainUtils.getTypeName(searchClass);
            JMenuItem menuItem = new JRadioButtonMenuItem(type, searchClass.equals(DEFAULT_SEARCH_CLASS));
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addCurrentConfig();
                    setResultType(searchClass);
                }
            });
            typeGroup.add(menuItem);
            typeCriteriaButton.addMenuItem(menuItem);
        }

        setResultType(defaultResultClass);
    }

    private void addCurrentConfig() {
        config.setDomainClassTitle(resultClass.getSimpleName(), titleInputBox.getText());
        config.setDomainClassSubtitle(resultClass.getSimpleName(), subtitleInputBox.getText());
    }

    private void setResultType(Class<? extends DomainObject> resultClass) {
        this.resultClass = resultClass;
        
        String type = DomainUtils.getTypeName(resultClass);
        typeCriteriaButton.setText("Result Type: " + type);

        // Install completion providers for the current result type
        CompletionProvider provider = createCompletionProvider(resultClass);
        titleInputBox.setCompletionProvider(provider);
        subtitleInputBox.setCompletionProvider(provider);

        String title = config.getDomainClassTitle(resultClass.getSimpleName());
        if (title==null) {
            titleInputBox.setText(DEFAULT_TITLE_VALUE);
        }
        else {
            titleInputBox.setText(title);
        }

        String subtitle = config.getDomainClassSubtitle(resultClass.getSimpleName());
        if (subtitle==null) {
            subtitleInputBox.setText(DEFAULT_SUBTITLE_VALUE);
        }
        else {
            subtitleInputBox.setText(subtitle);
        }
    }

    private CompletionProvider createCompletionProvider(Class<? extends DomainObject> resultClass) {
        List<DomainObjectAttribute> attrs = DomainUtils.getDisplayAttributes(resultClass);
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        for(DomainObjectAttribute attr : attrs) {
            provider.addCompletion(new ShorthandCompletion(provider, attr.getLabel(), "{"+attr.getLabel()+"}", ""));
        }
        return provider;
    }

    public int showDialog(Component parent) throws HeadlessException {
        ActivityLogHelper.logUserAction("IconGridViewerConfigDialog.showDialog");
        packAndShow();
        return returnValue;
    }

    private void saveAndClose() {

        UIUtils.setWaitingCursor(IconGridViewerConfigDialog.this);

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                addCurrentConfig();
                config.save();
            }

            @Override
            protected void hadSuccess() {
                UIUtils.setDefaultCursor(IconGridViewerConfigDialog.this);
                returnValue = CHOOSE_OPTION;
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                FrameworkAccess.handleException(error);
                UIUtils.setDefaultCursor(IconGridViewerConfigDialog.this);
                returnValue = ERROR_OPTION;
                setVisible(false);
            }
        };

        worker.execute();
    }

    public IconGridViewerConfiguration getConfig() {
        return config;
    }
}
