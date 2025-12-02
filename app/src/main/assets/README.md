# 模型文件说明

## MobileNetV3 模型文件

为了使用物品识别功能，您需要将 MobileNetV3 TensorFlow Lite 模型文件放在此目录中。

### 支持的模型版本

代码支持以下模型文件（按优先级排序）：
1. `mobilenet_v3_large.tflite` - Large 版本（推荐，精度更高）
2. `mobilenet_v3_small.tflite` - Small 版本（速度更快）
3. `mobilenet_v3_large_100_224_feature_vector.tflite` - Large 完整文件名
4. `mobilenet_v3_small_100_224.tflite` - Small 完整文件名

### 下载模型文件

#### Large 版本（推荐）
1. 访问 TensorFlow Hub: https://tfhub.dev/google/imagenet/mobilenet_v3_large_100_224/feature_vector/5
2. 下载 TensorFlow Lite 格式的模型
3. 将文件重命名为 `mobilenet_v3_large.tflite` 或保留原文件名
4. 将文件放在此目录 (`app/src/main/assets/`) 中

#### Small 版本（备选）
1. 访问 TensorFlow Hub: https://tfhub.dev/google/imagenet/mobilenet_v3_small_100_224/feature_vector/5
2. 下载 TensorFlow Lite 格式的模型
3. 将文件重命名为 `mobilenet_v3_small.tflite` 或保留原文件名
4. 将文件放在此目录 (`app/src/main/assets/`) 中

### 模型文件要求

- **输入尺寸**: 224x224
- **输出**: 特征向量（Large 版本约 1280 维，Small 版本约 1001 维）
- **格式**: TensorFlow Lite (.tflite)

### 版本对比

| 版本 | 特征向量大小 | 精度 | 速度 | 文件大小 |
|------|------------|------|------|---------|
| Large | ~1280 维 | 更高 | 较慢 | ~12-15 MB |
| Small | ~1001 维 | 较高 | 更快 | ~5-8 MB |

### 注意事项

- 代码会自动检测并支持 Large 和 Small 版本
- 如果没有模型文件，物品识别功能将无法使用
- 确保模型文件是 TensorFlow Lite 格式（.tflite）
- 代码会自动检测模型的实际输出维度，无需手动配置

