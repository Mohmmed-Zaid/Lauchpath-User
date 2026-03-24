// ============================================================
// FileRecordController.java
// ============================================================
package launchpath.userservice.co.controller;

import launchpath.userservice.co.dto.response.ApiResponseDTO;
import launchpath.userservice.co.dto.response.FileRecordResponseDTO;
import launchpath.userservice.co.entities.FileRecord;
import launchpath.userservice.co.enums.FileType;
import launchpath.userservice.co.mapper.UserMapper;
import launchpath.userservice.co.services.FileRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileRecordController {

    private final FileRecordService fileRecordService;
    private final UserMapper userMapper;

    // ══════════════════════════════════════════════════════════
    // GET ALL MY FILES
    // GET /api/v1/files/my
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my")
    public ResponseEntity<ApiResponseDTO<List<FileRecordResponseDTO>>> getMyFiles(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Get all files for userId: {}", userId);

        List<FileRecordResponseDTO> files = fileRecordService
                .getAllUserFiles(userId)
                .stream()
                .map(userMapper::toFileRecordResponseDTO)
                .toList();

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Files fetched successfully",
                        files
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // GET FILES BY TYPE
    // GET /api/v1/files/my?type=RESUME_PDF
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/type")
    public ResponseEntity<ApiResponseDTO<List<FileRecordResponseDTO>>> getMyFilesByType(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam FileType type) {

        log.info("Get files by type - userId: {}, type: {}", userId, type);

        List<FileRecordResponseDTO> files = fileRecordService
                .getUserFilesByType(userId, type)
                .stream()
                .map(userMapper::toFileRecordResponseDTO)
                .toList();

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "Files fetched successfully",
                        files
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // SAVE FILE RECORD
    // POST /api/v1/files
    // Called by resume-service after Cloudinary upload succeeds
    // ══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<ApiResponseDTO<FileRecordResponseDTO>> saveFileRecord(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String cloudinaryId,
            @RequestParam String url,
            @RequestParam FileType fileType,
            @RequestParam(required = false) Long sizeBytes) {

        log.info("Save file record - userId: {}, type: {}", userId, fileType);

        FileRecord saved = fileRecordService.saveFileRecord(
                userId,
                cloudinaryId,
                url,
                fileType,
                sizeBytes
        );

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "File record saved successfully",
                        userMapper.toFileRecordResponseDTO(saved)
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // COUNT FILES BY TYPE
    // GET /api/v1/files/my/count?type=RESUME_UPLOAD
    // Used to enforce free tier storage limits
    // ══════════════════════════════════════════════════════════

    @GetMapping("/my/count")
    public ResponseEntity<ApiResponseDTO<Long>> countFilesByType(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam FileType type) {

        long count = fileRecordService.countUserFilesByType(userId, type);

        return ResponseEntity.ok(
                ApiResponseDTO.success(
                        "File count fetched",
                        count
                )
        );
    }

    // ══════════════════════════════════════════════════════════
    // DELETE FILE RECORD
    // DELETE /api/v1/files/{id}
    // ══════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteFile(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Delete file - fileId: {}, userId: {}", id, userId);
        fileRecordService.deleteFileRecord(id, userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("File deleted successfully")
        );
    }

    // ══════════════════════════════════════════════════════════
    // DELETE ALL USER FILES — ADMIN / ACCOUNT DELETION
    // DELETE /api/v1/files/my/all
    // ══════════════════════════════════════════════════════════

    @DeleteMapping("/my/all")
    public ResponseEntity<ApiResponseDTO<Void>> deleteAllMyFiles(
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Delete all files for userId: {}", userId);
        fileRecordService.deleteAllUserFiles(userId);

        return ResponseEntity.ok(
                ApiResponseDTO.success("All files deleted successfully")
        );
    }
}
