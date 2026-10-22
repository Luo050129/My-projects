#pragma once
#include "../thirdparty/httplib.h"
#include"player.h"

#include<string>
#include<vector>
#include<thread>
#include<codecvt>
#include<chrono>// 作用：计时
#include<fstream>
#include<sstream>

std::string str_address;
httplib::Client* client = nullptr;

Atlas atlas_1P_idle_up;
Atlas atlas_1P_idle_down;
Atlas atlas_1P_idle_left;
Atlas atlas_1P_idle_right;
Atlas atlas_1P_run_up;
Atlas atlas_1P_run_down;
Atlas atlas_1P_run_left;
Atlas atlas_1P_run_right;

Atlas atlas_2P_idle_up;
Atlas atlas_2P_idle_down;
Atlas atlas_2P_idle_left;
Atlas atlas_2P_idle_right;
Atlas atlas_2P_run_up;
Atlas atlas_2P_run_down;
Atlas atlas_2P_run_left;
Atlas atlas_2P_run_right;

IMAGE img_main_menu_background;
IMAGE img_background;
IMAGE img_ui_1;
IMAGE img_ui_2;
IMAGE img_ui_3;
IMAGE img_ui_fight;
IMAGE img_ui_textbox;

//----------------
#include"path.h"
enum class Stage
{
	Waiting,
	Ready,
	Racing
};

int id_player = 0;							//玩家ID
std::string str_text;						//当前文本
std::atomic<int> progress_1 = -1;			//玩家1进度
std::atomic<int> progress_2 = -1;			//玩家2进度
std::vector<std::string> str_line_list;		//文本行列表
int val_countdown = 4;						//倒计时
Stage stage = Stage::Waiting;				//当前阶段
int num_total_char = 0;						//总字数

Path path = Path(
	{
		{842,842},{1322,842},{1322,442},
		{2762,442},{2762,842},{3162,842},
		{3162,1722},{2122,1722},{2122,1562},
		{842,1562},{842,842}
	}
);
int idx_line = 0; //文本行索引
int idx_char = 0; //文本字符索引

void load_resources(HWND hwnd)
{
	AddFontResourceEx(_T("resources/IPix.ttf"), FR_PRIVATE, NULL);

	atlas_1P_idle_up.load(_T("resources/hajimi_idle_back_%d.png"), 4);
	atlas_1P_idle_down.load(_T("resources/hajimi_idle_front_%d.png"), 4);
	atlas_1P_idle_left.load(_T("resources/hajimi_idle_left_%d.png"), 4);
	atlas_1P_idle_right.load(_T("resources/hajimi_idle_right_%d.png"), 4);
	atlas_1P_run_up.load(_T("resources/hajimi_run_back_%d.png"), 4);
	atlas_1P_run_down.load(_T("resources/hajimi_run_front_%d.png"), 4);
	atlas_1P_run_left.load(_T("resources/hajimi_run_left_%d.png"), 4);
	atlas_1P_run_right.load(_T("resources/hajimi_run_right_%d.png"), 4);

	atlas_2P_idle_up.load(_T("resources/manbo_idle_back_%d.png"), 4);
	atlas_2P_idle_down.load(_T("resources/manbo_idle_front_%d.png"), 4);
	atlas_2P_idle_left.load(_T("resources/manbo_idle_left_%d.png"), 4);
	atlas_2P_idle_right.load(_T("resources/manbo_idle_right_%d.png"), 4);
	atlas_2P_run_up.load(_T("resources/manbo_run_back_%d.png"), 4);
	atlas_2P_run_down.load(_T("resources/manbo_run_front_%d.png"), 4);
	atlas_2P_run_left.load(_T("resources/manbo_run_left_%d.png"), 4);
	atlas_2P_run_right.load(_T("resources/manbo_run_right_%d.png"), 4);

	loadimage(&img_main_menu_background, _T("resources/main_menu_background.png"));
	loadimage(&img_ui_1, _T("resources/ui_1.png"));
	loadimage(&img_ui_2, _T("resources/ui_2.png"));
	loadimage(&img_ui_3, _T("resources/ui_3.png"));
	loadimage(&img_ui_fight, _T("resources/ui_fight.png"));
	loadimage(&img_ui_textbox, _T("resources/ui_textbox.png"));
	loadimage(&img_background, _T("resources/background.png"));

	load_audio(_T("resources/bgm.mp3"), _T("bgm"));
	load_audio(_T("resources/1p_win.mp3"), _T("1p_win"));
	load_audio(_T("resources/2p_win.mp3"), _T("2p_win"));
	load_audio(_T("resources/click_1.mp3"), _T("click_1"));
	load_audio(_T("resources/click_2.mp3"), _T("click_2"));
	load_audio(_T("resources/click_3.mp3"), _T("click_3"));
	load_audio(_T("resources/click_4.mp3"), _T("click_4"));
	load_audio(_T("resources/ui_1.mp3"), _T("ui_1"));
	load_audio(_T("resources/ui_2.mp3"), _T("ui_2"));
	load_audio(_T("resources/ui_3.mp3"), _T("ui_3"));
	load_audio(_T("resoureces/ui_fight.mp3"), _T("ui_fight"));

	std::ifstream file("config.cfg");// 读取配置文件

	if (!file.good())
	{
		MessageBox(hwnd, _T("配置文件config.cfg缺失，程序无法运行！"), _T("错误"), MB_OK | MB_ICONERROR);
		exit(-1);
	}

	std::stringstream str_stream;
	str_stream << file.rdbuf();
	str_address = str_stream.str();
	std::cout << str_address << std::endl;
	file.close();
}

// 哈基米打字机
void login_to_server_hakimi_typewriter(HWND hwnd)
{
	client = new httplib::Client(str_address.c_str());
	client->set_keep_alive(true);//启用长连接

	httplib::Result result = client->Post("/login");
	if (!result || result->status != 200)
	{
		MessageBox(hwnd, _T("无法连接到服务器！"), _T("启动失败！哈气也没用！"), MB_OK | MB_ICONERROR);
		exit(-1);
	}
	std::cout << "连接成功" << std::endl;
	id_player = std::stoi(result->body);
	std::cout << "当前已有：" << id_player << std::endl;

	if (id_player <= 0)
	{
		MessageBox(hwnd, _T("房间已满！"), _T("加入失败！先吃点南北绿豆！"), MB_OK | MB_ICONERROR);
		exit(-1);
	}

	if (id_player == 1)
	{
		progress_1 = 0;
		std::cout << "玩家一已准备" << std::endl;
	}
	else {
		progress_2 = 0;
		std::cout << "玩家二已准备" << std::endl;
	}

	str_text = client->Post("/query_text")->body;

	std::stringstream str_stream(str_text);
	std::string str_line;
	while (std::getline(str_stream, str_line))
	{
		str_line_list.push_back(str_line);
		num_total_char += (int)str_line.length();
	}

	std::thread([&]
	() {
			while (true)//同步数据
			{
				using namespace std::chrono;

				//std::string route = (id_player == 1) ? "/update_1" : "update_2";
				//std::string body = std::to_string((id_player == 1) ? progress_1 : progress_2);
				std::string route; 
				std::string body;
				if (id_player == 1)
				{
					route = "/update_1";
					body = std::to_string(progress_1);
					//std::cout << "更新玩家一" << std::endl;
				}
				else {
					route = "/update_2";
					body = std::to_string(progress_2);
					//std::cout << "更新玩家二" << std::endl;
				}
				httplib::Result result = client->Post(route, body, "text/plain");

				if (result && result->status == 200)
				{
					int progress = std::stoi(result->body);
					(id_player == 1) ? (progress_2 = progress) : (progress_1 = progress);
				}

				std::this_thread::sleep_for(std::chrono::milliseconds(10));// 
			}
		}).detach();// 与主线程分离
}

void Sync_thread()
{

}