#pragma once
#include"vector2.h"

class Camera {
public:
	Camera() = default;
	~Camera() = default;

	void set_size(const Vector2& s)
	{
		size = s;
	}
	void set_position(const Vector2& pos)
	{
		position = pos;
	}
	Vector2 get_size() const
	{
		return size;
	}
	Vector2 get_position() const
	{
		return position;
	}
	void move(const Vector2& delta)
	{
		position = position + delta;
	}
	void look_at(const Vector2& target)
	{
		position = target - size / 2.0f;
	}
private:
	Vector2 position;
	Vector2 size;
};