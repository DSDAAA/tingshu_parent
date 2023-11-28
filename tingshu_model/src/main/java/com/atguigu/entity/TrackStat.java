package com.atguigu.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "TrackStat")
@TableName("track_stat")
public class TrackStat extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@Schema(description = "声音id")
	@TableField("track_id")
	private Long trackId;

	@Schema(description = "统计类型")
	@TableField("stat_type")
	private String statType;

	@Schema(description = "统计数目")
	@TableField("stat_num")
	private Integer statNum;

}