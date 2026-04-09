package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.vo.UsageRecordAccountSummaryVO;
import site.xlinks.ai.router.vo.UsageRecordModelSummaryVO;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UsageRecordAdminMapper extends BaseMapper<UsageRecord> {

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM usage_records u
            WHERE 1 = 1
            <if test='accountIds != null and accountIds.size() > 0'>
              AND u.account_id IN
              <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </if>
            <if test='modelCode != null and modelCode != ""'>
              AND u.model_code = #{modelCode}
            </if>
            <if test='providerCode != null and providerCode != ""'>
              AND u.provider_code = #{providerCode}
            </if>
            <if test='usageType != null and usageType != ""'>
              AND u.usage_type = #{usageType}
            </if>
            <if test='requestId != null and requestId != ""'>
              AND u.request_id = #{requestId}
            </if>
            <if test='responseStatus != null'>
              AND u.response_status = #{responseStatus}
            </if>
            <if test='startAt != null'>
              AND u.created_at <![CDATA[>=]]> #{startAt}
            </if>
            <if test='endAt != null'>
              AND u.created_at <![CDATA[<=]]> #{endAt}
            </if>
            </script>
            """)
    long countFlow(@Param("accountIds") List<Long> accountIds,
                   @Param("modelCode") String modelCode,
                   @Param("providerCode") String providerCode,
                   @Param("usageType") String usageType,
                   @Param("requestId") String requestId,
                   @Param("responseStatus") Integer responseStatus,
                   @Param("startAt") LocalDateTime startAt,
                   @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT u.*
            FROM usage_records u
            WHERE 1 = 1
            <if test='accountIds != null and accountIds.size() > 0'>
              AND u.account_id IN
              <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </if>
            <if test='modelCode != null and modelCode != ""'>
              AND u.model_code = #{modelCode}
            </if>
            <if test='providerCode != null and providerCode != ""'>
              AND u.provider_code = #{providerCode}
            </if>
            <if test='usageType != null and usageType != ""'>
              AND u.usage_type = #{usageType}
            </if>
            <if test='requestId != null and requestId != ""'>
              AND u.request_id = #{requestId}
            </if>
            <if test='responseStatus != null'>
              AND u.response_status = #{responseStatus}
            </if>
            <if test='startAt != null'>
              AND u.created_at <![CDATA[>=]]> #{startAt}
            </if>
            <if test='endAt != null'>
              AND u.created_at <![CDATA[<=]]> #{endAt}
            </if>
            ORDER BY u.created_at DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<UsageRecord> selectFlow(@Param("accountIds") List<Long> accountIds,
                                 @Param("modelCode") String modelCode,
                                 @Param("providerCode") String providerCode,
                                 @Param("usageType") String usageType,
                                 @Param("requestId") String requestId,
                                 @Param("responseStatus") Integer responseStatus,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt,
                                 @Param("offset") long offset,
                                 @Param("limit") long limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM (
              SELECT u.account_id
              FROM usage_records u
              WHERE 1 = 1
              <if test='accountIds != null and accountIds.size() > 0'>
                AND u.account_id IN
                <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                  #{id}
                </foreach>
              </if>
              <if test='modelCode != null and modelCode != ""'>
                AND u.model_code = #{modelCode}
              </if>
              <if test='providerCode != null and providerCode != ""'>
                AND u.provider_code = #{providerCode}
              </if>
              <if test='usageType != null and usageType != ""'>
                AND u.usage_type = #{usageType}
              </if>
              <if test='startAt != null'>
                AND u.created_at <![CDATA[>=]]> #{startAt}
              </if>
              <if test='endAt != null'>
                AND u.created_at <![CDATA[<=]]> #{endAt}
              </if>
              GROUP BY u.account_id
            ) t
            </script>
            """)
    long countAccountSummary(@Param("accountIds") List<Long> accountIds,
                             @Param("modelCode") String modelCode,
                             @Param("providerCode") String providerCode,
                             @Param("usageType") String usageType,
                             @Param("startAt") LocalDateTime startAt,
                             @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT
              u.account_id AS accountId,
              COALESCE(c.username, c.email, c.phone, CAST(u.account_id AS CHAR)) AS accountName,
              c.phone AS accountPhone,
              c.email AS accountEmail,
              COUNT(*) AS requestCount,
              COALESCE(SUM(u.prompt_tokens), 0) AS promptTokens,
              COALESCE(SUM(u.completion_tokens), 0) AS completionTokens,
              COALESCE(SUM(u.total_tokens), 0) AS totalTokens,
              COALESCE(SUM(u.cache_hit_tokens), 0) AS cacheHitTokens,
              COALESCE(SUM(u.total_cost), 0) AS totalCost,
              COALESCE(AVG(u.latency_ms), 0) AS avgLatencyMs
            FROM usage_records u
            LEFT JOIN customer_accounts c ON c.id = u.account_id
            WHERE 1 = 1
            <if test='accountIds != null and accountIds.size() > 0'>
              AND u.account_id IN
              <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </if>
            <if test='modelCode != null and modelCode != ""'>
              AND u.model_code = #{modelCode}
            </if>
            <if test='providerCode != null and providerCode != ""'>
              AND u.provider_code = #{providerCode}
            </if>
            <if test='usageType != null and usageType != ""'>
              AND u.usage_type = #{usageType}
            </if>
            <if test='startAt != null'>
              AND u.created_at <![CDATA[>=]]> #{startAt}
            </if>
            <if test='endAt != null'>
              AND u.created_at <![CDATA[<=]]> #{endAt}
            </if>
            GROUP BY u.account_id, c.username, c.email, c.phone
            ORDER BY totalCost DESC, requestCount DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<UsageRecordAccountSummaryVO> selectAccountSummary(@Param("accountIds") List<Long> accountIds,
                                                           @Param("modelCode") String modelCode,
                                                           @Param("providerCode") String providerCode,
                                                           @Param("usageType") String usageType,
                                                           @Param("startAt") LocalDateTime startAt,
                                                           @Param("endAt") LocalDateTime endAt,
                                                           @Param("offset") long offset,
                                                           @Param("limit") long limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM (
              SELECT u.model_id, u.model_code
              FROM usage_records u
              WHERE 1 = 1
              <if test='accountIds != null and accountIds.size() > 0'>
                AND u.account_id IN
                <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                  #{id}
                </foreach>
              </if>
              <if test='modelCode != null and modelCode != ""'>
                AND u.model_code = #{modelCode}
              </if>
              <if test='providerCode != null and providerCode != ""'>
                AND u.provider_code = #{providerCode}
              </if>
              <if test='usageType != null and usageType != ""'>
                AND u.usage_type = #{usageType}
              </if>
              <if test='startAt != null'>
                AND u.created_at <![CDATA[>=]]> #{startAt}
              </if>
              <if test='endAt != null'>
                AND u.created_at <![CDATA[<=]]> #{endAt}
              </if>
              GROUP BY u.model_id, u.model_code
            ) t
            </script>
            """)
    long countModelSummary(@Param("accountIds") List<Long> accountIds,
                           @Param("modelCode") String modelCode,
                           @Param("providerCode") String providerCode,
                           @Param("usageType") String usageType,
                           @Param("startAt") LocalDateTime startAt,
                           @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT
              u.model_id AS modelId,
              u.model_code AS modelCode,
              MAX(u.model_name) AS modelName,
              COUNT(*) AS requestCount,
              COALESCE(SUM(u.prompt_tokens), 0) AS promptTokens,
              COALESCE(SUM(u.completion_tokens), 0) AS completionTokens,
              COALESCE(SUM(u.total_tokens), 0) AS totalTokens,
              COALESCE(SUM(u.cache_hit_tokens), 0) AS cacheHitTokens,
              COALESCE(SUM(u.total_cost), 0) AS totalCost,
              COALESCE(AVG(u.latency_ms), 0) AS avgLatencyMs
            FROM usage_records u
            WHERE 1 = 1
            <if test='accountIds != null and accountIds.size() > 0'>
              AND u.account_id IN
              <foreach item='id' collection='accountIds' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </if>
            <if test='modelCode != null and modelCode != ""'>
              AND u.model_code = #{modelCode}
            </if>
            <if test='providerCode != null and providerCode != ""'>
              AND u.provider_code = #{providerCode}
            </if>
            <if test='usageType != null and usageType != ""'>
              AND u.usage_type = #{usageType}
            </if>
            <if test='startAt != null'>
              AND u.created_at <![CDATA[>=]]> #{startAt}
            </if>
            <if test='endAt != null'>
              AND u.created_at <![CDATA[<=]]> #{endAt}
            </if>
            GROUP BY u.model_id, u.model_code
            ORDER BY totalCost DESC, requestCount DESC
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<UsageRecordModelSummaryVO> selectModelSummary(@Param("accountIds") List<Long> accountIds,
                                                       @Param("modelCode") String modelCode,
                                                       @Param("providerCode") String providerCode,
                                                       @Param("usageType") String usageType,
                                                       @Param("startAt") LocalDateTime startAt,
                                                       @Param("endAt") LocalDateTime endAt,
                                                       @Param("offset") long offset,
                                                       @Param("limit") long limit);
}
