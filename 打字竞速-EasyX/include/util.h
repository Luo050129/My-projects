#pragma once
#include"camera.h"

#include<easyx.h>

#pragma comment(lib,"WINMM.lib")
#pragma comment(lib,"MSIMG32.lib")

struct Rect
{
	int x, y;
	int w, h;
};

inline void putimage_ex(const Camera& cam, IMAGE* img, const Rect* rect_dst, const Rect* rect_src = nullptr)
{
	static BLENDFUNCTION bf = { AC_SRC_OVER, 0, 255, AC_SRC_ALPHA };

	const Vector2 cam_pos = cam.get_position();
	AlphaBlend(GetImageHDC(GetWorkingImage()),(int)(rect_dst->x - cam_pos.x),(int)(rect_dst->y - cam_pos.y),rect_dst->w,rect_dst->h,
		GetImageHDC(img),(rect_src ? rect_src->x : 0),(rect_src ? rect_src->y : 0),
		(rect_src ? rect_src->w : img->getwidth()), (rect_src ? rect_src->h : img->getheight()), bf);
}

inline void load_audio(LPCTSTR path,LPCTSTR id)
{
	static TCHAR str_cmd[512];
	_stprintf_s(str_cmd, _T("open %s alias %s"), path, id);
	mciSendString(str_cmd, NULL, 0, NULL);
}

inline void play_audio(LPCTSTR id, BOOL loop = FALSE)
{
	static TCHAR str_cmd[512];
	_stprintf_s(str_cmd, _T("play %s %s"), id, (loop ? _T("repeat") : _T("")));
	mciSendString(str_cmd, NULL, 0, NULL);
}

inline void stop_audio(LPCTSTR id)
{
	static TCHAR str_cmd[512];
	_stprintf_s(str_cmd, _T("stop %s"), id);
	mciSendString(str_cmd, NULL, 0, NULL);
}