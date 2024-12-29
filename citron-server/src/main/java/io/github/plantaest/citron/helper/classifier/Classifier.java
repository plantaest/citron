package io.github.plantaest.citron.helper.classifier;

import ai.onnxruntime.OnnxMap;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.github.plantaest.citron.config.model.ModelManager;
import io.github.plantaest.citron.enumeration.Model;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Classifier {

    @Inject
    ModelManager modelManager;

    public List<ClassificationResult> classify(List<HostnameFeature> hostnameFeatures, Model model) {
        List<ClassificationResult> classificationResults = new ArrayList<>();

        if (hostnameFeatures.isEmpty()) {
            return classificationResults;
        }

        try {
            var env = OrtEnvironment.getEnvironment();
            var session = env.createSession(modelManager.getModel(model), new OrtSession.SessionOptions());
            var featureValueRows = hostnameFeatures.stream()
                    .map(this::convertToFloatArray)
                    .toArray(float[][]::new);
            var tensor = OnnxTensor.createTensor(env, featureValueRows);
            var inputs = Map.of("float_input", tensor);

            try (OrtSession.Result results = session.run(inputs)) {
                if (results.get("output_label").isPresent() && results.get("output_probability").isPresent()) {
                    var onnxLabels = (long[]) results.get("output_label").get().getValue();
                    List<Long> labels = Arrays.stream(onnxLabels).boxed().toList();

                    @SuppressWarnings("unchecked")
                    var onnxProbs = (List<OnnxMap>) results.get("output_probability").get().getValue();
                    List<Float> probs = new ArrayList<>();
                    for (var prob : onnxProbs) {
                        probs.add((float) prob.getValue().get(1L));
                    }

                    for (int i = 0; i < labels.size(); i++) {
                        var classificationResult = ClassificationResultBuilder.builder()
                                .hostname(hostnameFeatures.get(i).hostname())
                                .label(labels.get(i))
                                .probability(probs.get(i))
                                .build();
                        classificationResults.add(classificationResult);
                    }
                }
            }

            return classificationResults;
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    private float[] convertToFloatArray(HostnameFeature hostnameFeature) {
        float[] floats = new float[20];

        // [0] open_page_rank
        // [1] open_page_rank_available
        // [2] aka_rank
        // [3] aka_rank_available
        // [4] tranco_rank
        // [5] tranco_rank_available
        // [6] majestic_million_rank
        // [7] majestic_million_rank_available
        // [8] cloudflare_radar_available
        // [9] has_special_word
        // [10] commercial_tld
        // [11] entertainment_tld
        // [12] gambling_tld
        // [13] suspicious_tld
        // [14] hostname_length
        // [15] dot_count
        // [16] digit_count
        // [17] is_ipv4
        // [18] is_top_domain
        // [19] is_top_private_domain

        floats[0] = (float) hostnameFeature.openPageRank();
        floats[1] = hostnameFeature.openPageRankAvailable() ? 1 : 0;
        floats[2] = hostnameFeature.akaRank();
        floats[3] = hostnameFeature.akaRankAvailable() ? 1 : 0;
        floats[4] = hostnameFeature.trancoRank();
        floats[5] = hostnameFeature.trancoRankAvailable() ? 1 : 0;
        floats[6] = hostnameFeature.majesticMillionRank();
        floats[7] = hostnameFeature.majesticMillionRankAvailable() ? 1 : 0;
        floats[8] = hostnameFeature.cloudflareRadarAvailable() ? 1 : 0;
        floats[9] = hostnameFeature.hasSpecialWord() ? 1 : 0;
        floats[10] = hostnameFeature.commercialTld() ? 1 : 0;
        floats[11] = hostnameFeature.entertainmentTld() ? 1 : 0;
        floats[12] = hostnameFeature.gamblingTld() ? 1 : 0;
        floats[13] = hostnameFeature.suspiciousTld() ? 1 : 0;
        floats[14] = hostnameFeature.hostnameLength();
        floats[15] = hostnameFeature.dotCount();
        floats[16] = hostnameFeature.digitCount();
        floats[17] = hostnameFeature.isIpv4() ? 1 : 0;
        floats[18] = hostnameFeature.isTopDomain() ? 1 : 0;
        floats[19] = hostnameFeature.isTopPrivateDomain() ? 1 : 0;

        return floats;
    }

}
