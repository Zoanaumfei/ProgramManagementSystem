package com.oryzem.programmanagementsystem.platform.documents;

public interface PortfolioDocumentStorageGateway {

    PreparedDocumentUpload prepareUpload(DocumentStorageObject document);

    PreparedDocumentDownload prepareDownload(DocumentStorageObject document);

    void assertObjectExists(DocumentStorageObject document);

    void deleteObject(DocumentStorageObject document);
}
