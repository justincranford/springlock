package com.springlock.lock.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/** Spring config for springlock. Scans {@code com.springlock.lock}. */
@Configuration
@ComponentScan("com.springlock.lock")
public class LockServiceConfiguration {
}
