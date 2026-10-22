#pragma once
#include <cmath>

class Vector2 {
public:
	float x = 0;
	float y = 0;
public:
	Vector2() = default;
	Vector2(float x, float y) : x(x), y(y) {}
	Vector2 operator+(const Vector2& other) const {
		return Vector2(x + other.x, y + other.y);
	}
	void operator+=(const Vector2& other) {
		x += other.x;
		y += other.y;
	}
	Vector2 operator-(const Vector2& other) const {
		return Vector2(x - other.x, y - other.y);
	}
	void operator-=(const Vector2& other) {
		x -= other.x;
		y -= other.y;
	}
	Vector2 operator*(float scalar) const {
		return Vector2(x * scalar, y * scalar);
	}
	float operator*(const Vector2& other) const {
		return x * other.x + y * other.y;
	}
	void operator*=(float scalar) {
		x *= scalar;
		y *= scalar;
	}
	Vector2 operator/(float scalar) const {
		return Vector2(x / scalar, y / scalar);
	}
	void operator/=(float scalar) {
		x /= scalar;
		y /= scalar;
	}
	float dot(const Vector2& other) const {
		return x * other.x + y * other.y;
	}
	float length() const {
		return std::sqrt(x * x + y * y);
	}
	Vector2 normalized() const {
		float len = length();
		if (len == 0) return Vector2(0, 0);
		return Vector2(x / len, y / len);
	}
	bool approx(const Vector2& other, float epsilon = 0.0001f) const {
		return (*this-other).length() < epsilon;
	}// approx用来判断两个向量是否近似相等
};