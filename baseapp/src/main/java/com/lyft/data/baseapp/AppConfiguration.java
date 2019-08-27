package com.lyft.data.baseapp;

import io.dropwizard.Configuration;
import java.util.List;
import lombok.Data;

@Data
public class AppConfiguration extends Configuration {

  // List of Modules with FQCN (Fully Qualified Class Name)
  private List<String> modules;

  // List of ManagedApps with FQCN (Fully Qualified Class Name)
  private List<String> managedApps;
}
