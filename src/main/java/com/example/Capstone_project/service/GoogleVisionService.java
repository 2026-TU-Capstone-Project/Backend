package com.example.Capstone_project.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.protobuf.ByteString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleVisionService {

    // 🔑 핵심: 코드가 직접 리소스 폴더의 키 파일을 찾아옵니다!
    private ImageAnnotatorSettings getSettings() throws IOException {
        // src/main/resources/google-key.json 파일을 읽음
        InputStream keyStream = new ClassPathResource("google-key.json").getInputStream();
        GoogleCredentials credentials = GoogleCredentials.fromStream(keyStream);

        return ImageAnnotatorSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();
    }

    // 1. 태그 추출 (기존 기능)
    public List<String> extractLabels(MultipartFile file) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());
        Image image = Image.newBuilder().setContent(imgBytes).build();

        Feature feature = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();
        requests.add(request);

        // 변경점: settings를 넣어서 클라이언트 생성
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(getSettings())) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            List<String> tags = new ArrayList<>();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.printf("Error: %s\n", res.getError().getMessage());
                    return tags;
                }
                // 정확도 85% 이상만 가져오기
                res.getLabelAnnotationsList().forEach(annotation -> {
                    if (annotation.getScore() >= 0.85) {
                        tags.add(annotation.getDescription());
                    }
                });
            }
            return tags;
        }
    }

    // 2. 색깔 추출 (기존 기능 유지)
    public String extractDominantColor(MultipartFile file) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());
        Image image = Image.newBuilder().setContent(imgBytes).build();

        Feature feature = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feature)
                .setImage(image)
                .build();
        requests.add(request);

        // 변경점: settings를 넣어서 클라이언트 생성
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(getSettings())) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            AnnotateImageResponse res = response.getResponses(0);

            if (res.hasError()) {
                return "Unknown";
            }

            ColorInfo colorInfo = res.getImagePropertiesAnnotation().getDominantColors().getColors(0);
            return getColorNameFromRgb(colorInfo.getColor().getRed(), colorInfo.getColor().getGreen(), colorInfo.getColor().getBlue());
        }
    }

    // 색깔 변환기 (그대로 유지)
    private String getColorNameFromRgb(float r, float g, float b) {
        String[] colorNames = {"Black", "White", "Grey", "Red", "Orange", "Yellow", "Green", "Blue", "Navy", "Purple", "Pink", "Brown", "Beige"};
        int[][] colorRgb = {
                {0, 0, 0}, {255, 255, 255}, {128, 128, 128}, {255, 0, 0}, {255, 165, 0}, {255, 255, 0},
                {0, 128, 0}, {0, 0, 255}, {0, 0, 128}, {128, 0, 128}, {255, 192, 203}, {165, 42, 42}, {245, 245, 220}
        };

        String closestColor = "Unknown";
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < colorNames.length; i++) {
            double distance = Math.sqrt(Math.pow(r - colorRgb[i][0], 2) + Math.pow(g - colorRgb[i][1], 2) + Math.pow(b - colorRgb[i][2], 2));
            if (distance < minDistance) {
                minDistance = distance;
                closestColor = colorNames[i];
            }
        }
        return closestColor;
    }
}