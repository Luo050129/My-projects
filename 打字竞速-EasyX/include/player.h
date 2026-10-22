#pragma once
#include"animation.h"

const float Player_Interval_Idle = 0.1f;

class Player
{
public:
	enum class Face {
		Up, Down, Left, Right
	};

public:
	Player(Atlas* atlas_idle_up, Atlas* atlas_idle_down, Atlas* atlas_idle_left, Atlas* atlas_idle_right,
		Atlas* atlas_run_up, Atlas* atlas_run_down, Atlas* atlas_run_left, Atlas* atlas_run_right)
	{
		anim_idle_up.set_loop(true);
		anim_idle_up.set_interval(Player_Interval_Idle);
		anim_idle_up.add_frame(atlas_idle_up);

		anim_idle_down.set_loop(true);
		anim_idle_down.set_interval(Player_Interval_Idle);
		anim_idle_down.add_frame(atlas_idle_down);

		anim_idle_left.set_loop(true);
		anim_idle_left.set_interval(Player_Interval_Idle);
		anim_idle_left.add_frame(atlas_idle_left);

		anim_idle_right.set_loop(true);
		anim_idle_right.set_interval(Player_Interval_Idle);
		anim_idle_right.add_frame(atlas_idle_right);

		anim_run_up.set_loop(true);
		anim_run_up.set_interval(Player_Interval_Idle);
		anim_run_up.add_frame(atlas_run_up);

		anim_run_down.set_loop(true);
		anim_run_down.set_interval(Player_Interval_Idle);
		anim_run_down.add_frame(atlas_run_down);

		anim_run_left.set_loop(true);
		anim_run_left.set_interval(Player_Interval_Idle);
		anim_run_left.add_frame(atlas_run_left);

		anim_run_right.set_loop(true);
		anim_run_right.set_interval(Player_Interval_Idle);
		anim_run_right.add_frame(atlas_run_right);
	}

	~Player() = default;
	void on_update(float delta)
	{
		if (!position.approx(pos_target))
			velocity = (pos_target - position).normalized() * Speed_Run;
		else
			velocity = Vector2(0, 0);

		if((pos_target - position).length() <= velocity.length()*delta)
			position = pos_target;
		else
			position += velocity * delta;

		if (velocity.approx(Vector2(0, 0)))
		{
			switch (facing)
			{
			case Player::Face::Up:    current_anim = &anim_idle_up;    break;
			case Player::Face::Down:  current_anim = &anim_idle_down;  break;
			case Player::Face::Left:  current_anim = &anim_idle_left;  break;
			case Player::Face::Right: current_anim = &anim_idle_right; break;
			}
		}
		else 
		{
			if (abs(velocity.y) >= 0.0001f)
				facing = (velocity.y < 0.0f) ? Face::Up : Face::Down;
			if(abs(velocity.x)>=0.0001f)
				facing = (velocity.x < 0.0f) ? Face::Left : Face::Right;

			switch (facing)
			{
			case Player::Face::Up:    current_anim = &anim_run_up;    break;
			case Player::Face::Down:  current_anim = &anim_run_down;  break;
			case Player::Face::Left:  current_anim = &anim_run_left;  break;
			case Player::Face::Right: current_anim = &anim_run_right; break;
			}
		}
		if (!current_anim)return;
		current_anim->set_position(position);
		current_anim->on_update(delta);
	}

	void on_render(const Camera& cam)
	{
		if (!current_anim)return;
		current_anim->on_render(cam);
	}

	void set_position(const Vector2& pos)
	{
		position = pos;
	}
	const Vector2& get_position() const
	{
		return position;
	}
	void set_target_position(const Vector2& pos)
	{
		pos_target = pos;
	}
private:
	const float Speed_Run = 100.0f;//每秒100像素
private:
	Vector2 position;
	Vector2 velocity;
	Vector2 pos_target;

	Face facing = Face::Down;

	Animation anim_idle_up;
	Animation anim_idle_down;
	Animation anim_idle_left;
	Animation anim_idle_right;
	Animation anim_run_up;
	Animation anim_run_down;
	Animation anim_run_left;
	Animation anim_run_right;
	Animation* current_anim = nullptr;
	
};