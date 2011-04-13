/*
 * Copyright, 1999-2010, SALESFORCE.com 
 * All Rights Reserved
 * Company Confidential
 */
package com.force.cliforce;

import com.beust.jcommander.IStringConverter;
import jline.internal.Configuration;

import java.io.File;

/**
 * TildeAwareFileConverter
 *
 * @author sclasen
 */
public class TildeAwareFileConverter implements IStringConverter<java.io.File> {

    @Override
    public File convert(String value) {
        File homeDir = new File(System.getProperty("user.home"));
        String translated = value;
        // Special character: ~ maps to the user's home directory
        if (translated.startsWith("~" + separator())) {
            translated = homeDir.getPath() + translated.substring(1);
        } else if (translated.startsWith("~")) {
            translated = homeDir.getParentFile().getAbsolutePath();
        } else if (!(translated.startsWith(separator()))) {
            String cwd = getUserDir().getAbsolutePath();
            translated = cwd + separator() + translated;
        }
        return new File(translated);
    }

    protected String separator() {
        return File.separator;
    }

    protected File getUserHome() {
        return Configuration.getUserHome();
    }

    protected File getUserDir() {
        return new File(".");
    }
}
