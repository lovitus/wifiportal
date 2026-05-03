package com.fanli.wifiportal;

interface IPortalShell {
    String exec(String command) = 1;
    void destroy() = 16777114;
}
