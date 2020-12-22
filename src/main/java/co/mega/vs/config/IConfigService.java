package co.mega.vs.config;


import co.mega.vs.config.impl.Config;

public interface IConfigService {
	
	void loadConfig();
	
	Config getConfig();

}
