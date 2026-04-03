package site.xlinks.ai.router.client.vo;

import lombok.Data;

@Data
public class ContactChannelConfigVO {

    private Long id;

    private String channelType;

    private String title;

    private String description;

    private String contactValue;

    private String actionLink;

    private String actionLabel;
}