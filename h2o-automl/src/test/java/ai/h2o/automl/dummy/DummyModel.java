package ai.h2o.automl.dummy;

import hex.Model;
import hex.ModelBuilder;
import hex.ModelMetrics;
import hex.ModelMetricsBinomial;
import water.Key;
import water.util.IcedHashMap;

import java.util.Random;
import java.util.function.Function;

public class DummyModel extends Model<DummyModel, DummyModel.DummyModelParameters, DummyModel.DummyModelOutput>{

    public static class DummyModelParameters extends Model.Parameters {

        public transient Function<double[], double[]> _predict;
        public String _tag = null;
        public IcedHashMap<String, String> _moreParams = new IcedHashMap<>();

        @Override
        public String algoName() {
            return "dummy";
        }

        @Override
        public String fullName() {
            return "Dummy";
        }

        @Override
        public String javaName() {
            return DummyModel.class.getName();
        }

        @Override
        public long progressUnits() {
            return 1;
        }
    }

    public static class DummyModelOutput extends Model.Output {

        public IcedHashMap<String, String> _moreProperties = new IcedHashMap<>();

        boolean _supervised = true;

        public DummyModelOutput() {
            super();
        }

        public DummyModelOutput(ModelBuilder b) {
            super(b);
        }

        @Override
        public boolean isSupervised() {
            return _supervised;
        }
    }


    public DummyModel(String key) {
        super(Key.make(key), new DummyModelParameters(), new DummyModelOutput());
    }

    public DummyModel(Key<DummyModel> key, DummyModelParameters parameters, DummyModelOutput output) {
        super(key, parameters, output);
    }

    @Override
    public ModelMetrics.MetricBuilder makeMetricBuilder(String[] domain) {
        return new ModelMetricsBinomial.MetricBuilderBinomial(domain);
    }

    @Override
    protected double[] score0(double[] data, double[] preds) {
        return _parms._predict == null ? preds : _parms._predict.apply(data);
    }
}
