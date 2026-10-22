#pragma once

#include"atlas.h"
#include"time.h"
#include"vector2.h"
#include"util.h"

#include<vector>
#include<functional>

class Animation {
public:
	Animation()
	{
		timer.set_one_shot(false);
		timer.set_on_timeout(
			[&]()
			{
				idx_frame++;
				if (idx_frame >= frame_list.size())
				{
					idx_frame = is_loop ? 0 : frame_list.size() - 1;
					if (!is_loop && on_finished)
						on_finished();
				}
			});
	}
	~Animation() = default;

	void reset()
	{
		timer.restart();
		idx_frame = 0;
	}

	void set_position(const Vector2& pos)
	{
		position = pos;
	}

	void set_loop(bool loop)
	{
		is_loop = loop;
	}

	void set_interval(float interval)
	{
		timer.set_wait_time(interval);
	}

	void set_on_finished(const std::function<void()>& func)
	{
		on_finished = func;
	}
	
	void add_frame(IMAGE* img,int num_h)
	{
		int width = img->getwidth();
		int height = img->getheight();
		int width_frame = width / num_h;

		for (int i = 0; i < num_h; i++)
		{
			Rect rect_src;
			rect_src.x = i * width_frame;
			rect_src.y = 0;
			rect_src.w = width_frame;
			rect_src.h = height;
			frame_list.emplace_back(rect_src, img);
		}
	}
	void add_frame(Atlas* atlas)
	{
		for(int i = 0; i < atlas->get_size(); i++)
		{
			IMAGE* img = atlas->get_image(i);
			int width = img->getwidth();
			int height = img->getheight();

			Rect rect_src;
			rect_src.x = 0;
			rect_src.y = 0;
			rect_src.w = width;
			rect_src.h = height;

			frame_list.emplace_back(rect_src, img);
		}
	}

	void on_update(float delta_time)
	{
		timer.on_update(delta_time);
	}

	void on_render(const Camera& cam)
	{
		const Frame& frame = frame_list[idx_frame];

		Rect rect_dst;
		rect_dst.x = (int)position.x - frame.rect_src.w / 2;
		rect_dst.y = (int)position.y - frame.rect_src.h / 2;
		rect_dst.w = frame.rect_src.w;
		rect_dst.h = frame.rect_src.h;

		putimage_ex(cam, frame.image, &rect_dst, &frame.rect_src);
	}
private:
	struct Frame {
		Rect rect_src;
		IMAGE* image = nullptr;

		Frame() = default;
		Frame(const Rect& rect, IMAGE* img) :rect_src(rect), image(img) {}
		~Frame() = default;
	};
private:
	Timer timer;
	Vector2 position;
	bool is_loop = true;
	size_t idx_frame = 0;
	std::vector<Frame> frame_list;
	std::function<void()> on_finished;
};