package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import site.xlinks.ai.router.entity.ProviderModel;

import java.util.List;

/**
 * Provider model mapping mapper.
 */
@Mapper
public interface ProviderModelMapper extends BaseMapper<ProviderModel> {

    @Select("""
            SELECT id, provider_id, model_id, provider_model_code, provider_model_name, status, deleted, remark,
                   created_at, updated_at, create_by, update_by
            FROM provider_models
            WHERE provider_id = #{providerId}
              AND model_id = #{modelId}
            LIMIT 1
            """)
    ProviderModel selectIncludingDeletedByProviderAndModel(@Param("providerId") Long providerId,
                                                           @Param("modelId") Long modelId);

    @Select("""
            <script>
            SELECT id, provider_id, model_id, provider_model_code, provider_model_name, status, deleted, remark,
                   created_at, updated_at, create_by, update_by
            FROM provider_models
            WHERE provider_id = #{providerId}
              AND model_id IN
              <foreach collection="modelIds" item="modelId" open="(" separator="," close=")">
                #{modelId}
              </foreach>
            </script>
            """)
    List<ProviderModel> selectIncludingDeletedByProviderAndModels(@Param("providerId") Long providerId,
                                                                  @Param("modelIds") List<Long> modelIds);

    @Update("""
            UPDATE provider_models
            SET provider_model_code = #{providerModelCode},
                provider_model_name = #{providerModelName},
                status = #{status},
                deleted = 0,
                remark = #{remark},
                update_by = #{updateBy},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int restoreDeleted(@Param("id") Long id,
                       @Param("providerModelCode") String providerModelCode,
                       @Param("providerModelName") String providerModelName,
                       @Param("status") Integer status,
                       @Param("remark") String remark,
                       @Param("updateBy") String updateBy);
}
