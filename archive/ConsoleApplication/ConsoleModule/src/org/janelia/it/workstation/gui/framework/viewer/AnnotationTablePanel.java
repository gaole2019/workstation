package org.janelia.it.workstation.gui.framework.viewer;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.options.OptionConstants;
import org.janelia.it.workstation.gui.dialogs.AnnotationBuilderDialog;
import org.janelia.it.workstation.gui.framework.outline.OntologyOutline;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.framework.table.DynamicColumn;
import org.janelia.it.workstation.gui.framework.table.DynamicTable;
import org.janelia.it.workstation.gui.util.MouseForwarder;
import org.janelia.it.workstation.gui.util.MouseHandler;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel that shows a bunch of annotations in a table.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AnnotationTablePanel extends JPanel implements AnnotationView {

    private static final Logger log = LoggerFactory.getLogger(AnnotationTablePanel.class);
    
    private static final String COLUMN_KEY = "Annotation Term";
    private static final String COLUMN_VALUE = "Annotation Value";

    private DynamicTable dynamicTable;
    private JLabel summaryLabel;

    private List<OntologyAnnotation> annotations = new ArrayList<>();

    public AnnotationTablePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        refresh();
    }

    @Override
    public List<OntologyAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public void setAnnotations(List<OntologyAnnotation> annotations) {
        if (annotations == null) {
            this.annotations = new ArrayList<>();
        }
        else {
            this.annotations = annotations;
        }
        refresh();
    }

    @Override
    public void removeAnnotation(OntologyAnnotation annotation) {
        annotations.remove(annotation);
        refresh();
    }

    @Override
    public void addAnnotation(OntologyAnnotation annotation) {
        annotations.add(annotation);
        refresh();
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
        if (preferredSize.height == ImagesPanel.MIN_TABLE_HEIGHT) {
            removeAll();
            add(summaryLabel, BorderLayout.CENTER);
        }
        else {
            removeAll();
            add(dynamicTable, BorderLayout.CENTER);
        }
    }

    private void refresh() {

        summaryLabel = new JLabel(annotations.size() + " annotation" + (annotations.size() > 1 ? "s" : ""));
        summaryLabel.setOpaque(false);
        summaryLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        summaryLabel.addMouseListener(new MouseHandler() {
            @Override
            protected void doubleLeftClicked(MouseEvent e) {
                SessionMgr.getSessionMgr().setModelProperty(
                        OptionConstants.ANNOTATION_TABLES_HEIGHT_PROPERTY, ImagesPanel.DEFAULT_TABLE_HEIGHT);
                e.consume();
            }

        });

        dynamicTable = new DynamicTable(false, true) {

            @Override
            public Object getValue(Object userObject, DynamicColumn column) {

                OntologyAnnotation annotation = (OntologyAnnotation) userObject;
                if (null != annotation) {
                    if (column.getName().equals(COLUMN_KEY)) {
                        return annotation.getKeyString();
                    }
                    if (column.getName().equals(COLUMN_VALUE)) {
                        return annotation.getValueString();
                    }
                }
                return null;
            }

            @Override
            protected JPopupMenu createPopupMenu(MouseEvent e) {

                if (dynamicTable.getCurrentRow() == null) {
                    return null;
                }

                Object userObject = dynamicTable.getCurrentRow().getUserObject();
                OntologyAnnotation annotation = (OntologyAnnotation) userObject;

                return getPopupMenu(e, annotation);
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                if (col != 1) {
                    return null;
                }

                // TODO: implement custom editors for each ontology term type
                return null;
            }
        };

        dynamicTable.getScrollPane().setWheelScrollingEnabled(false);
        dynamicTable.getScrollPane().addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                getParent().dispatchEvent(e);
            }
        });

        dynamicTable.getTable().addMouseListener(new MouseForwarder(this, "DynamicTable->AnnotationTablePanel"));

        dynamicTable.addColumn(COLUMN_KEY, COLUMN_KEY, true, false, false, true);
        dynamicTable.addColumn(COLUMN_VALUE, COLUMN_VALUE, true, false, false, true);

        for (OntologyAnnotation annotation : annotations) {
            dynamicTable.addRow(annotation);
        }

        dynamicTable.updateTableModel();
        removeAll();
        add(dynamicTable, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private void deleteAnnotation(final OntologyAnnotation toDelete) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                ModelMgr.getModelMgr().removeAnnotation(toDelete.getId());
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation",error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotation", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    private void deleteAnnotations(final List<OntologyAnnotation> toDeleteList) {

        Utils.setWaitingCursor(SessionMgr.getMainFrame());

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {
                for (OntologyAnnotation toDelete : toDeleteList) {
                    ModelMgr.getModelMgr().removeAnnotation(toDelete.getId());
                }
            }

            @Override
            protected void hadSuccess() {
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
            }

            @Override
            protected void hadError(Throwable error) {
                log.error("Error deleting annotation",error);
                Utils.setDefaultCursor(SessionMgr.getMainFrame());
                JOptionPane.showMessageDialog(AnnotationTablePanel.this, "Error deleting annotations", "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        worker.execute();
    }

    private JPopupMenu getPopupMenu(final MouseEvent e, final OntologyAnnotation annotation) {

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setLightWeightPopupEnabled(true);

        JTable target = (JTable) e.getSource();
        if (target.getSelectedRow() < 0) {
            return null;
        }

        JTable table = dynamicTable.getTable();

        ListSelectionModel lsm = table.getSelectionModel();
        if (lsm.getMinSelectionIndex() == lsm.getMaxSelectionIndex()) {

            JMenuItem titleItem = new JMenuItem(annotation.getEntity().getName());
            titleItem.setEnabled(false);
            popupMenu.add(titleItem);

            JMenuItem copyMenuItem = new JMenuItem("  Copy to Clipboard");
            copyMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Transferable t = new StringSelection(annotation.getEntity().getName());
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
                }
            });
            popupMenu.add(copyMenuItem);

            if (EntityUtils.hasWriteAccess(annotation.getEntity(), SessionMgr.getSubjectKeys())) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotation");
                deleteItem.addActionListener(new ActionListener() {
                @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteAnnotation(annotation);
                    }
                });
                popupMenu.add(deleteItem);
            }

            if (null != annotation.getValueString()) {
                JMenuItem editItem = new JMenuItem("  Edit Annotation");
                editItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        AnnotationBuilderDialog dialog = new AnnotationBuilderDialog();
                        dialog.setAnnotationValue(annotation.getValueString());
                        dialog.setVisible(true);
                        String value = dialog.getAnnotationValue();
                        if (null == value) {
                            value = "";
                        }
                        annotation.setValueString(value);
                        annotation.getEntity().setValueByAttributeName(EntityConstants.ATTRIBUTE_ANNOTATION_ONTOLOGY_VALUE_TERM, value);
                        String tmpName = annotation.getEntity().getName();
                        String namePrefix = tmpName.substring(0, tmpName.indexOf("=") + 2);
                        annotation.getEntity().setName(namePrefix + value);
                        try {
                            Entity tmpAnnotatedEntity = ModelMgr.getModelMgr().getEntityById(annotation.getTargetEntityId());
                            ModelMgr.getModelMgr().saveOrUpdateAnnotation(tmpAnnotatedEntity, annotation.getEntity());
                        }
                        catch (Exception e1) {
                            log.error("Error editing annotation",e1);
                            SessionMgr.getSessionMgr().handleException(e1);
                        }
                    }
                });

                popupMenu.add(editItem);
            }

            JMenuItem detailsItem = new JMenuItem("  View Details");
            detailsItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    OntologyOutline.viewAnnotationDetails(annotation);
                }
            });
            popupMenu.add(detailsItem);

        }
        else {
            JMenuItem titleMenuItem = new JMenuItem("(Multiple Items Selected)");
            titleMenuItem.setEnabled(false);
            popupMenu.add(titleMenuItem);

            final List<OntologyAnnotation> toDeleteList = new ArrayList<>();
            for (int i : table.getSelectedRows()) {
                int mi = table.convertRowIndexToModel(i);
                toDeleteList.add(annotations.get(mi));
            }

            if (SessionMgr.getUsername().equals(annotation.getOwner())) {
                JMenuItem deleteItem = new JMenuItem("  Delete Annotations");
                deleteItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        deleteAnnotations(toDeleteList);
                    }
                });
                popupMenu.add(deleteItem);
            }
        }

        return popupMenu;
    }

}