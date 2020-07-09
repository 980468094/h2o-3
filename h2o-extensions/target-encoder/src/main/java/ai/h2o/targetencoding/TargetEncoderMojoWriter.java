package ai.h2o.targetencoding;

import ai.h2o.targetencoding.TargetEncoderModel.TargetEncoderOutput;
import hex.Model;
import hex.ModelMojoWriter;
import hex.genmodel.algos.targetencoder.EncodingMap;
import hex.genmodel.algos.targetencoder.EncodingMaps;
import water.fvec.Frame;

import java.io.IOException;
import java.util.Map;

public class TargetEncoderMojoWriter extends ModelMojoWriter {

  @SuppressWarnings("unused")  // Called through reflection in ModelBuildersHandler
  public TargetEncoderMojoWriter() {
  }

  public TargetEncoderMojoWriter(Model model) {
    super(model);
  }

  @Override
  public String mojoVersion() {
    return "1.00";
  }

  @Override
  protected void writeModelData() throws IOException {
    writeTargetEncodingInfo();
    writeTargetEncodingMap();
  }

  @Override
  protected void writeExtraInfo() {
    // Do nothing
  }

  /**
   * Writes target encoding's extra info
   */
  private void writeTargetEncodingInfo() throws IOException {
    TargetEncoderOutput output = ((TargetEncoderModel) model)._output;
    TargetEncoderModel.TargetEncoderParameters teParams = output._parms;
    writekv("with_blending", teParams._blending);
    if(teParams._blending) {
      writekv("inflection_point", teParams._k);
      writekv("smoothing", teParams._f);
    }
    writekv("priorMean", output._prior_mean);
    
    Map<String, Integer> _teColumnNameToMissingValuesPresence = output._column_name_to_missing_val_presence;
    startWritingTextFile("feature_engineering/target_encoding/te_column_name_to_missing_values_presence.ini");
    for(Map.Entry<String, Integer> entry: _teColumnNameToMissingValuesPresence.entrySet()) {
      writelnkv(entry.getKey(), entry.getValue().toString());
    }
    finishWritingTextFile();
  }

  /**
   * Writes encoding map into the file line by line
   */
  private void writeTargetEncodingMap() throws IOException {
    TargetEncoderOutput targetEncoderOutput = ((TargetEncoderModel) model)._output;
    Map<String, Frame> targetEncodingMap = targetEncoderOutput._target_encoding_map;

    groupEncodingsByFoldColumnIfNeeded(targetEncoderOutput, targetEncodingMap);

    // We need to convert map only here - before writing to MOJO. Everywhere else having encoding maps based on Frames is fine.
    EncodingMaps convertedEncodingMap = TargetEncoderFrameHelper.convertEncodingMapValues(targetEncodingMap);
    startWritingTextFile("feature_engineering/target_encoding/encoding_map.ini");
    for (Map.Entry<String, EncodingMap> columnEncodingsMap : convertedEncodingMap.entrySet()) {
      writeln("[" + columnEncodingsMap.getKey() + "]");
      EncodingMap encodings = columnEncodingsMap.getValue();
      for (Map.Entry<Integer, int[]> catLevelInfo : encodings.entrySet()) {
        int[] numAndDenom = catLevelInfo.getValue(); // FIXME: for regression, num is not an int...
        writelnkv(catLevelInfo.getKey().toString(), numAndDenom[0] + " " + numAndDenom[1]);
      }
    }
    finishWritingTextFile();
  }

  /**
   * For transforming (making predictions) non-training data we don't need `te folds` in our encoding maps 
   */
  private void groupEncodingsByFoldColumnIfNeeded(TargetEncoderOutput targetEncoderOutput, Map<String, Frame> targetEncodingMap) {
    String foldColumn = targetEncoderOutput._parms._fold_column;
    if (foldColumn != null) {
      try {
        for (Map.Entry<String, Frame> encodingMapEntry : targetEncodingMap.entrySet()) {
          String teColumn = encodingMapEntry.getKey();
          Frame encodingsWithFolds = encodingMapEntry.getValue();
          Frame encodingsWithoutFolds = TargetEncoder.groupEncodingsByCategory(encodingsWithFolds, encodingsWithFolds.find(teColumn) , true);
          targetEncodingMap.put(teColumn, encodingsWithoutFolds);
          encodingsWithFolds.delete();
        }
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to group encoding maps by fold column", ex);
      }
    }
  }
}
