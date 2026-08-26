package vn.gpg.unionportal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gpg.unionportal.model.DomainEnums.MemberDocumentType;

@Entity
@Table(name = "member_documents")
@Getter
@Setter
@NoArgsConstructor
public class MemberDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private MemberDocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "uploaded_by", nullable = false, length = 150)
    private String uploadedBy;
}
