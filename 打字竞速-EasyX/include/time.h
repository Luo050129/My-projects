#pragma once
#include<functional>

class Timer {
public:
	Timer() = default;
	~Timer() = default;

	void restart()
	{
		pass_time = 0;
		shotted = false;
	}

	void set_wait_time(float t)
	{
		wait_time = t;
	}

	void set_one_shot(bool os)
	{
		one_shot = os;
	}

	void set_on_timeout(const std::function<void()>& timeout_func)
	{
		on_timeout = timeout_func;
	}

	void pause()
	{
		paused = true;
	}

	void resume()
	{
		paused = false;
	}

	void on_update(float delta_time)
	{
		if (paused)
			return;
		pass_time += delta_time;
		if (pass_time >= wait_time)
		{
			bool can_shot = (!one_shot || (one_shot && shotted));
			shotted = true;
			if (can_shot && on_timeout)
			{
				on_timeout();
			}
			pass_time = wait_time;
		}
	}
private:
	float wait_time = 0;
	float pass_time = 0;
	bool one_shot = false;
	bool shotted = false;
	bool paused = false;
	std::function<void()> on_timeout = nullptr;
};