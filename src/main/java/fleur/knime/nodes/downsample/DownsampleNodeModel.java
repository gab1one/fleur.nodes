package fleur.knime.nodes.downsample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import fleur.core.data.FCSFrame;
import fleur.core.sample.DownSample;
import fleur.core.sample.DownSampleMethods;
import fleur.core.transforms.TransformSet;
import fleur.core.utils.BitSetUtils;
import fleur.core.utils.FCSUtilities;
import fleur.knime.core.NodeUtilities;
import fleur.knime.data.type.cell.fcs.FCSFrameFileStoreDataCell;

/**
 * This is the model implementation of Downsample.
 * 
 *
 * @author Aaron Hart
 */
public class DownsampleNodeModel extends NodeModel {

  DownsampleNodeSettings mSettings = new DownsampleNodeSettings();
  private int taskCount = -1;
  private int currentTask;
  private int targetColumnIndex = -1;
private TransformSet transformSet;

  /**
   * Constructor for the node model.
   */
  protected DownsampleNodeModel() {
    super(1,1);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
      throws InvalidSettingsException {
    if (mSettings.getSelectedColumn()==null){
      Arrays.asList(inSpecs[0].getColumnNames())
      .stream()
      .map(inSpecs[0]::getColumnSpec)
      .filter(spec -> spec.getType().equals(FCSFrameFileStoreDataCell.TYPE))
      .findFirst()
      .ifPresent(column -> mSettings.setSelectedColumn(column.getName()));    
    }
    return createSpecs(inSpecs[0]);
  }
  
  private DataTableSpec[] createSpecs(DataTableSpec dataTableSpec) {
    return new DataTableSpec[] {dataTableSpec};
  }


  /**
   * {@inheritDoc}
   */
  @Override
  protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
      final ExecutionContext exec) throws Exception {
    
    exec.setProgress(0.01, "Collecting input rows.");
    
    DataTableSpec[] outSpecs = createSpecs(inData[0].getDataTableSpec());
    String columnName = mSettings.getSelectedColumn();
	DataColumnProperties props = outSpecs[0].getColumnSpec(columnName).getProperties();
    if (props.containsProperty(NodeUtilities.KEY_TRANSFORM_MAP)){
        transformSet = TransformSet.loadFromProtoString(props.getProperty(NodeUtilities.KEY_TRANSFORM_MAP));
    } else {
    	throw new CanceledExecutionException("Unable to parse transform map");
    }
    
    ArrayList<DataRow> data = new ArrayList<>();
    targetColumnIndex = inData[0].getSpec().findColumnIndex(mSettings.getSelectedColumn());
    inData[0].forEach(data::add);
    
    currentTask = 0;
    taskCount = data.size();
    BufferedDataContainer container = exec.createDataContainer(outSpecs[0]);
    exec.setProgress(0.15, "Downsampling data.");
    exec.checkCanceled();
    data
      .parallelStream()
      .map(row -> createRow(row, exec))
      .forEach(row -> writeRow(row, container, exec));
    
    container.close();
    return new BufferedDataTable[]{container.getTable()}; 
  }

  private DataRow createRow(DataRow inRow, ExecutionContext exec){
    FCSFrame fileFrame = ((FCSFrameFileStoreDataCell) inRow.getCell(targetColumnIndex)).getFCSFrameValue();
    final FCSFrame outFrame = downSample(fileFrame);
    final String fsName = NodeUtilities.getFileStoreName(outFrame);
    final DataCell[] outCells = new DataCell[inRow.getNumCells()];
    synchronized (exec) {
      FileStoreFactory factory = FileStoreFactory.createWorkflowFileStoreFactory(exec);
      FileStore fs;
      FCSFrameFileStoreDataCell fileCell;
      int size = -1;
      try {
        fs = factory.createFileStore(fsName);
        size = NodeUtilities.writeFrameToFilestore(outFrame, fs);
        fileCell = new FCSFrameFileStoreDataCell(fs, outFrame, size);
      } catch (IOException e) {
        getLogger().error("Unable to create file store");
        fs = null;
        fileCell = null;
      }

      for (int j = 0; j < outCells.length; j++) {
        if (j == targetColumnIndex&&size >=0) {
          outCells[j] = fileCell;
        } else  if (j == targetColumnIndex && (size ==-1||fs==null)){
          outCells[j] = new MissingCell("Unable to write filestore.");
        } else {
          outCells[j] = inRow.getCell(j);
        }
      }
    }
    return new DefaultRow(inRow.getKey(), outCells);
  }
  
  private FCSFrame downSample(FCSFrame dataFrame) {
    String referenceSubsetName = mSettings.getReferenceSubset();
    FCSFrame refFrame;
    if (referenceSubsetName.equals(DownsampleNodeSettings.DEFAULT_REFERENCE_SUBSET)){
    	refFrame = dataFrame;
    } else {
    	BitSet referenceMask = dataFrame.getFilteredFrame(mSettings.getReferenceSubset(), true);
    	refFrame = FCSUtilities.filterFrame(referenceMask, dataFrame);
    }
	BitSet mask = null;
    List<String> dimensionNames = Arrays.asList(mSettings.getDimensionNames());
    if (mSettings.getSampleMethod().equals(DownSampleMethods.RANDOM)){
      mask = BitSetUtils.getShuffledMask(refFrame.getRowCount(), mSettings.getCeiling());
    }else if (mSettings.getSampleMethod().equals(DownSampleMethods.DENSITY_DEPENDENT)){
      mask = DownSample.densityDependent(refFrame, dimensionNames, mSettings.getCeiling(), transformSet); 
    }
    if (mask!=null){
      return FCSUtilities.filterFrame(mask, refFrame);
    } else {
      getLogger().error("Downsampling failed due to invalid sample method.");
      return dataFrame;
    }
  }

  private synchronized void writeRow(DataRow row, BufferedDataContainer container, ExecutionContext exec) {
    String message = "Writing: " + ((FCSFrameFileStoreDataCell) row.getCell(targetColumnIndex)).getFCSFrameMetadata().getDisplayName(); 
    exec.setProgress(currentTask/(double)taskCount, message);
    try {
      exec.checkCanceled();
    } catch (CanceledExecutionException e) {
      e.printStackTrace();
    }
    container.addRowToTable(row);
    currentTask++;   
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {
    mSettings.load(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {/*noop*/}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {
    mSettings.save(settings);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
  }
}