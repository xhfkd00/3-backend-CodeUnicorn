package com.codeUnicorn.codeUnicorn.service

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.codeUnicorn.codeUnicorn.constant.ExceptionMessage
import com.codeUnicorn.codeUnicorn.domain.file.FileUploadLog
import com.codeUnicorn.codeUnicorn.domain.file.FileUploadLogRepository
import com.codeUnicorn.codeUnicorn.dto.FileUploadLogDto
import com.codeUnicorn.codeUnicorn.exception.FileNotExistException
import com.codeUnicorn.codeUnicorn.exception.FileNotSupportedException
import com.codeUnicorn.codeUnicorn.exception.FileUploadFailException
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile

@Service
class S3FileUploadService {
    @Autowired
    private lateinit var amazonS3Client: AmazonS3Client

    @Value("\${cloud.aws.s3.bucket}")
    private lateinit var bucket: String

    @Autowired
    private lateinit var fileUploadLogRepository: FileUploadLogRepository

    @Throws(RuntimeException::class)
    fun uploadFile(file: MultipartFile): String {
        log.info { "file: $file" }
        // 파일 존재 여부 검증
        validateFileExist(file)
        // 파일 형식 검증
        val contentType: String = file.contentType ?: ""
        validateFileSupportedContentType(contentType)
        // 파일 최대 용량 초과 여부 검증은 스프링부트에 설정해 준 값에 의해 자동으로 진행된다.

        // 파일 이름 정의
        val fileName: String = UUID.randomUUID().toString()

        // 파일 S3 에 업로드
        return putS3(file, fileName)
    }

    // S3로 업로드
    @Throws(IOException::class)
    private fun putS3(uploadFile: MultipartFile, fileName: String): String {
        val objectMetadata = ObjectMetadata()
        objectMetadata.contentType = uploadFile.contentType

        val inputStream: InputStream = uploadFile.inputStream

        try {
            val objectResult = amazonS3Client.putObject(PutObjectRequest(bucket, "images/$fileName", inputStream, objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead))
            log.info { "objectResult: $objectResult" }
        } catch (e: IOException) {
            throw FileUploadFailException(ExceptionMessage.FILE_UPLOAD_FAIL)
        }

        val originalFilename: String = uploadFile.originalFilename ?: ""
        val type: String = StringUtils.getFilenameExtension(originalFilename) ?: ""
        val directoryPath: String = amazonS3Client.getUrl(bucket, "$fileName.$type").toString()
        val size: Int = uploadFile.size.toInt()

        // 파일 정보 로그 테이블에 파일 정보 저장
        // FileUploadLogDto: File 정보 담는 객체, FileUploadLog: File 정보를 DB 에 저장하는 형태
        val fileUploadLogDto = FileUploadLogDto(directoryPath, fileName, type, size)
        val fileUploadLog: FileUploadLog = fileUploadLogDto.toEntity()

        fileUploadLogRepository.save(fileUploadLog)

        return directoryPath
    }

    // 파일 존재 여부 검증
    @Throws(FileNotExistException::class)
    fun validateFileExist(file: MultipartFile) {
        if (file.isEmpty) {
            throw FileNotExistException(ExceptionMessage.FILE_NOT_EXIST)
        }
    }

    // 파일 형식 지원 여부 검증
    @Throws(FileNotSupportedException::class)
    fun validateFileSupportedContentType(contentType: String) {
        log.info { "contentType: $contentType" }
        log.info { !(contentType.contains("jpg")) && !(contentType.contains("jpeg")) && !(contentType.contains("png")) }
        if (!(contentType.contains("jpg")) && !(contentType.contains("jpeg")) && !(contentType.contains("png"))) {
            throw FileNotSupportedException(ExceptionMessage.FILE_NOT_SUPPORTED)
        }
    }
}