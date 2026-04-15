package com.oryzem.programmanagementsystem.modules.documentmanagement.api;

import com.oryzem.programmanagementsystem.modules.documentmanagement.application.FinalizeDocumentUploadUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.GenerateDocumentDownloadUrlUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.GetDocumentDetailUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.InitiateDocumentUploadUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.ListDocumentsByContextUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.application.SoftDeleteDocumentUseCase;
import com.oryzem.programmanagementsystem.modules.documentmanagement.domain.DocumentContextType;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUser;
import com.oryzem.programmanagementsystem.platform.authorization.AuthenticatedUserMapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DocumentManagementController {

    private final AuthenticatedUserMapper authenticatedUserMapper;
    private final InitiateDocumentUploadUseCase initiateDocumentUploadUseCase;
    private final FinalizeDocumentUploadUseCase finalizeDocumentUploadUseCase;
    private final ListDocumentsByContextUseCase listDocumentsByContextUseCase;
    private final GetDocumentDetailUseCase getDocumentDetailUseCase;
    private final GenerateDocumentDownloadUrlUseCase generateDocumentDownloadUrlUseCase;
    private final SoftDeleteDocumentUseCase softDeleteDocumentUseCase;

    public DocumentManagementController(
            AuthenticatedUserMapper authenticatedUserMapper,
            InitiateDocumentUploadUseCase initiateDocumentUploadUseCase,
            FinalizeDocumentUploadUseCase finalizeDocumentUploadUseCase,
            ListDocumentsByContextUseCase listDocumentsByContextUseCase,
            GetDocumentDetailUseCase getDocumentDetailUseCase,
            GenerateDocumentDownloadUrlUseCase generateDocumentDownloadUrlUseCase,
            SoftDeleteDocumentUseCase softDeleteDocumentUseCase) {
        this.authenticatedUserMapper = authenticatedUserMapper;
        this.initiateDocumentUploadUseCase = initiateDocumentUploadUseCase;
        this.finalizeDocumentUploadUseCase = finalizeDocumentUploadUseCase;
        this.listDocumentsByContextUseCase = listDocumentsByContextUseCase;
        this.getDocumentDetailUseCase = getDocumentDetailUseCase;
        this.generateDocumentDownloadUrlUseCase = generateDocumentDownloadUrlUseCase;
        this.softDeleteDocumentUseCase = softDeleteDocumentUseCase;
    }

    @PostMapping("/document-contexts/{contextType}/{contextId}/documents/uploads")
    public UploadSessionResponse initiateUpload(
            Authentication authentication,
            @PathVariable DocumentContextType contextType,
            @PathVariable String contextId,
            @Valid @RequestBody InitiateDocumentUploadRequest request) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return UploadSessionResponse.from(initiateDocumentUploadUseCase.execute(
                contextType,
                contextId,
                request.originalFilename(),
                request.contentType(),
                request.sizeBytes(),
                request.checksumSha256(),
                actor));
    }

    @PostMapping("/documents/{documentId}/finalize-upload")
    public DocumentResponse finalizeUpload(
            Authentication authentication,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return DocumentResponse.from(finalizeDocumentUploadUseCase.execute(documentId, actor));
    }

    @GetMapping("/document-contexts/{contextType}/{contextId}/documents")
    public List<DocumentResponse> listByContext(
            Authentication authentication,
            @PathVariable DocumentContextType contextType,
            @PathVariable String contextId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return listDocumentsByContextUseCase.execute(contextType, contextId, actor).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse getDocument(
            Authentication authentication,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return DocumentResponse.from(getDocumentDetailUseCase.execute(documentId, actor));
    }

    @PostMapping("/documents/{documentId}/download-url")
    public DownloadUrlResponse generateDownloadUrl(
            Authentication authentication,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return DownloadUrlResponse.from(generateDocumentDownloadUrlUseCase.execute(documentId, actor));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<DocumentResponse> softDelete(
            Authentication authentication,
            @PathVariable String documentId) {
        AuthenticatedUser actor = authenticatedUserMapper.from(authentication);
        return ResponseEntity.ok(DocumentResponse.from(softDeleteDocumentUseCase.execute(documentId, actor)));
    }
}
