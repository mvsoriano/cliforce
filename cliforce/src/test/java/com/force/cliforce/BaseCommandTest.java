package com.force.cliforce;

import java.io.IOException;

public abstract class BaseCommandTest extends BaseTest {

    public abstract String getPluginArtifact();
    
    public abstract Plugin getPlugin();
 
    public boolean isInternal(){
        return true;
    }
    
    @Override
    public void setupCLIForce(CLIForce c) throws IOException {
      c.installPlugin(getPluginArtifact(), "LATEST", getPlugin(), isInternal());
    }

}
