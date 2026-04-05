import Foundation

/// Parity with `ml/FeatureExtractor.kt` — load `.tflite` from bundle and run inference (integrate TensorFlowLite pod/SPM on Mac).
enum TensorFlowLiteService {
    static func extractFeatureCode(from imageData: Data) async throws -> String {
        // Placeholder: wire TensorFlowLite Swift interpreter when dependency is added to the Xcode project.
        throw NSError(
            domain: "TFLite",
            code: 0,
            userInfo: [NSLocalizedDescriptionKey: "Add TensorFlowLite to the target; then map input/output tensors to Android pipeline."]
        )
    }
}
