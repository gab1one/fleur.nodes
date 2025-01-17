package fleur.knime.nodes.statistics;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import fleur.core.gates.GateUtilities;

@SuppressWarnings("serial")
public class StatEditorDialog extends JDialog {

  private static final String DIALOG_TITLE = "Define statistic";
  /**
   * A modal dialog from which new chart definitions will be created and existing charts may be
   * edited
   */

  protected JPanel settingsPanel;
  protected JPanel contentPanel;
  private StatSpec localSpec;
  private boolean isOK = false;
  private List<String> dimensionList;
  private JPanel statDetailsPanel;
  private List<String> subsetList;
  /**
   * 
   * @param topFrame - used to set modality of dialog.
   * @param subsetList - list of subsets on which to calculate a statistic
   * @param dimensionList - list of dimension short names for which a stat may be calculated
   * @param spec - existing statistic definition to edit
   */
  public StatEditorDialog(Window topFrame, List<String> dimensionList, List<String> subsetList, StatSpec spec) {
    super(topFrame);
    setModal(true);
    this.subsetList = subsetList;
    this.dimensionList = dimensionList;
    if (spec != null) {
      localSpec = spec.copy();
    } else {
      Optional<String> optDD = dimensionList.stream().findAny();
      if (optDD.isPresent()){
        localSpec = new StatSpec(optDD.get(), GateUtilities.UNGATED_SUBSET_ID, StatType.MEDIAN, null);
      } else {
        localSpec = new StatSpec(null, GateUtilities.UNGATED_SUBSET_ID, StatType.COUNT, null);
      }
    }
    
    // populate the dialog
    JPanel content = createContentPanel();
    getContentPane().add(content);
    pack();
    setLocationRelativeTo(getParent());
    setTitle(DIALOG_TITLE);
  }

  public StatEditorDialog(Window topFrame, List<String> dimensionList, List<String> subsetList) {
    this(topFrame, dimensionList, subsetList, null);
  }

  private JButton createCancelButton() {
    JButton button = new JButton();
    button.setText("Cancel");
    button.addActionListener(e -> setVisible(false));
    return button;
  }

  private JPanel createContentPanel() {    
    // Create the panel
    contentPanel = new JPanel(new BorderLayout());
    JComboBox<StatType> statBox = new JComboBox<>(StatType.values());
    statBox.addActionListener(e -> {
     localSpec.setStatType((StatType)statBox.getSelectedItem());
     updateStatDetailsPanel();
    });
    contentPanel.add(statBox, BorderLayout.NORTH);

    updateStatDetailsPanel();

    JButton okButton = createOkButton();
    JButton cancelButton = createCancelButton();
    final JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    contentPanel.add(statDetailsPanel, BorderLayout.CENTER);
    
    contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    return contentPanel;
  }

  protected void updateStatDetailsPanel() {
    if (statDetailsPanel!=null){
      contentPanel.remove(statDetailsPanel);
    }
    
    StatType localType = localSpec.getStatType();
    if (localType.equals(StatType.CV)||localType.equals(StatType.MEAN)||
        localType.equals(StatType.MEDIAN) || localType.equals(StatType.STDEV)){
      statDetailsPanel = createSingleDimensionStatPanel();
    } else if (localSpec.getStatType().equals(StatType.FREQUENCY)){
      statDetailsPanel = createFrequencyDefinitionPanel();
    } else if (localSpec.getStatType().equals(StatType.PERCENTILE)){
      statDetailsPanel = createPercentilePanel();
    }
    contentPanel.add(statDetailsPanel);
    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private JPanel createPercentilePanel() {
    JPanel panel = new JPanel();
    
    JComboBox<String> subsetSelection = new JComboBox<>(
        subsetList.toArray(new String[subsetList.size()]));
    
    subsetSelection.addActionListener(e ->localSpec.setSubsetID(
        (String) subsetSelection.getSelectedItem()));
    
    panel.add(subsetSelection);
    JComboBox<String> dimensionSelector = new JComboBox<>(
        dimensionList.toArray(new String[dimensionList.size()]));
    dimensionSelector.addActionListener(
        e -> localSpec.setDimension((String) dimensionSelector.getSelectedItem()));
    panel.add(dimensionSelector);
    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(50, 0, 100, 1);
    JSpinner percentileSpinner = new JSpinner(spinnerModel);
    percentileSpinner.addChangeListener(e -> localSpec.setPercentile(percentileSpinner.getValue()));
    panel.add(percentileSpinner);
    return panel;
  }

  private JPanel createFrequencyDefinitionPanel() {
    JPanel panel = new JPanel();
    JComboBox<String> childSelectionBox = new JComboBox<>(subsetList.toArray(new String[subsetList.size()]));
    childSelectionBox.addActionListener(e -> {
        String selectedSubset = (String) childSelectionBox.getSelectedItem();
        localSpec.setSubsetID(selectedSubset);   
    });
    panel.add(childSelectionBox);
    return panel;
  }

  private JPanel createSingleDimensionStatPanel() {
    JPanel panel = new JPanel();
    JComboBox<String> subsetSelection = new JComboBox<>(subsetList.toArray(new String[subsetList.size()]));
    subsetSelection.addActionListener(e -> localSpec.setSubsetID((String) subsetSelection.getSelectedItem()));
    JComboBox<String> dimensionSelector = new JComboBox<>(dimensionList.toArray(new String[dimensionList.size()]));
    dimensionSelector.addActionListener(e -> localSpec.setDimension(
        (String) dimensionSelector.getSelectedItem()));
    panel.add(dimensionSelector);
    return panel;
  }

  private JButton createOkButton() {
    JButton button = new JButton();
    button.setText("Ok");
    button.addActionListener(e -> {
        isOK = true;
        setVisible(false);
    });
    return button;
  }

  public StatSpec getStatSpec() {
    return localSpec;
  }
  
  public boolean isOK(){
    return this.isOK;
  } 
}