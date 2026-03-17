package com.exam.exam_system.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * QR Kod generasiya servisi
 *
 * QR kodun içindəki məlumat:
 *   http://localhost:8080/api/qr/scan?token=<qrToken>
 *
 * Nəzarətçi telefonu ilə oxudanda bu link açılır,
 * ekranda tələbənin adı və qrupu görünür.
 */
@Service
public class QrCodeService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * QR kod şəklini byte array kimi qaytarır (PNG formatında)
     */
    public byte[] generateQrCode(String qrToken, int width, int height)
            throws WriterException, IOException {

        String qrContent = baseUrl + "/api/qr/scan?token=" + qrToken;

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent, BarcodeFormat.QR_CODE, width, height);

        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
        return outputStream.toByteArray();
    }
}