package fleur.knime.nodes.portToTableCell;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

import fleur.core.data.FCSFrame;
import fleur.knime.core.NodeUtilities;
import fleur.knime.data.type.cell.fcs.FCSFrameFileStoreDataCell;
import fleur.knime.data.type.cell.fcs.FCSFrameMetaData;
import fleur.knime.ports.fcs.FCSFramePortObject;

/**
 * This is the model implementation of ColumnStoreToTableCell. Converts a
 *
 * @author Landysh Co.
 */
public class ColumnStoreToTableCellNodeModel extends NodeModel {

  /**
   * Constructor for the node model.
   */
  protected ColumnStoreToTableCellNodeModel() {
    super(new PortType[] {FCSFramePortObject.TYPE}, new PortType[] {BufferedDataTable.TYPE});
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected DataTableSpec[] configure(final PortObjectSpec[] inSpecs)
      throws InvalidSettingsException {
    final DataColumnSpecCreator colSpec =
        new DataColumnSpecCreator("Listmode Data", FCSFrameFileStoreDataCell.TYPE);
    // colSpec.setProperties(new DataColumnProperties(Collections.singletonMap(
    // DataValueRenderer.PROPERTY_PREFERRED_RENDERER, CellLineageRenderer.DESCRIPTION)));
    final org.knime.core.data.DataTableSpec spec = new DataTableSpec(colSpec.createSpec());
    return new DataTableSpec[] {spec};
  }

  /**
   * {@inheritDoc}
   * 
   * @throws CanceledExecutionException
   */
  @Override
  protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec)
      throws CanceledExecutionException {
    // create the output container
    final DataColumnSpec colSpecs =
        new DataColumnSpecCreator("Listmode Data", FCSFrameFileStoreDataCell.TYPE).createSpec();
    final DataTableSpec spec = new DataTableSpec(colSpecs);
    final BufferedDataContainer container = exec.createDataContainer(spec);

    // Create the file store
    final FileStoreFactory fsFactory = FileStoreFactory.createWorkflowFileStoreFactory(exec);
    // get the data and write it to the container
    final FCSFramePortObject port = (FCSFramePortObject) inData[0];
    DataCell[] dataCells;
    try {
      FCSFrame df = port.getColumnStore(exec);
      String fsName = NodeUtilities.getFileStoreName(df);
      FileStore fs = fsFactory.createFileStore(fsName);
      int size = NodeUtilities.writeFrameToFilestore(df, fs);
      FCSFrameMetaData metaData = new FCSFrameMetaData(df, size);
      FCSFrameFileStoreDataCell cell = new FCSFrameFileStoreDataCell(fs, metaData);
      dataCells = new DataCell[] {cell};
      final DataRow dataRow = new DefaultRow("Row 0", dataCells);
      container.addRowToTable(dataRow);

      // cleanup and create the table
      container.close();
      final BufferedDataTable table = container.getTable();
      return new BufferedDataTable[] {table};
    } catch (IOException e) {
      String message = "Unable to read data from port object.";
      getLogger().error(message, e);
      throw new CanceledExecutionException(message);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
      throws InvalidSettingsException {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveInternals(final File internDir, final ExecutionMonitor exec)
      throws IOException, CanceledExecutionException {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void saveSettingsTo(final NodeSettingsWO settings) {}

  /**
   * {@inheritDoc}
   */
  @Override
  protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {}
}
