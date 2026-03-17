package led.mega.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("menu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu implements Persistable<String> {

    @Id
    @Column("menu_id")
    private String menuId;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    private String name;
    private String url;
    private String icon;
    @Column("sort_order")
    private int sortOrder;
    @Column("parent_id")
    private String parentId;
    @Column("required_role")
    private String requiredRole;

    @Builder.Default
    @Column("is_enabled")
    private boolean isEnabled = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public String getId() {
        return menuId;
    }

    @Override
    public boolean isNew() {
        return isNew || menuId == null;
    }
}
