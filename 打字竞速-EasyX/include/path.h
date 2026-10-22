#pragma once
#include"vector2.h"

#include<vector>

class Path
{
public:
	Path(const std::vector<Vector2>& points)
	{
		point_list = points;

		for(size_t i=1;i<point_list.size();++i)
		{
			float seg_len = (point_list[i]-point_list[i-1]).length();
			segment_len_list.push_back(seg_len);
			total_length += seg_len;
		}
	}
	~Path() = default;

	Vector2 get_position_at_progress(float progess)const
	{
		if(progess<=0.0f)
			return point_list.front();
		if (progess >= 1.0f)
			return point_list.back();

		float len_at_progess = progess * total_length;

		float len_accum = 0.0f;//累计长度
		for(size_t i=0;i<segment_len_list.size();++i)
		{
			if (len_accum + segment_len_list[i] >= len_at_progess)//在这一段内
			{
				float seg_progess = (len_at_progess - len_accum) / segment_len_list[i];
				return point_list[i] + (point_list[i + 1] - point_list[i]) * seg_progess;
			}
			len_accum += segment_len_list[i];
		}
	}
private:
	float total_length = 0.0f;
	std::vector<Vector2> point_list;
	std::vector<float> segment_len_list;//每一段的长度
};