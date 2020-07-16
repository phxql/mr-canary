package de.mkammerer.mrcanary.netty.admin;

import de.mkammerer.mrcanary.netty.admin.route.impl.CanariesRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.DefaultRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StartCanaryRoute;
import de.mkammerer.mrcanary.netty.admin.route.impl.StatusRoute;
import lombok.Value;

@Value
public
class Routes {
    DefaultRoute defaultRoute;
    StatusRoute statusRoute;
    CanariesRoute canariesRoute;
    StartCanaryRoute startCanaryRoute;
}
