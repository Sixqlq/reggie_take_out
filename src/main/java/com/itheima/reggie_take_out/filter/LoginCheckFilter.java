package com.itheima.reggie_take_out.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.reggie_take_out.common.BaseContext;
import com.itheima.reggie_take_out.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否完成登录
 */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    // 路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // 1. 获取本次请求路径的URI
        String requestURI = request.getRequestURI();

        log.info("拦截到请求：{}", requestURI);

        // 定义不需要处理的请求路径
        String[] urls = new String[] {
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/sendMsg", // 移动端发送短信
                "/user/login" //移动端登录
        };

        // 2. 判断本次请求是否需要处理（登录、登出及访问静态资源请求不需要处理，处理的是后端接口数据的请求）
        boolean check = check(urls, requestURI);

        // 3. 如果不需要处理，则直接放行
        if(check) {
            log.info("本次请求{}不需要处理", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 4-1. 判断后台用户登录状态，如果已经登录，则直接放行
        if(request.getSession().getAttribute("employee") != null) {
            log.info("用户已经登录，用户id为：{}", request.getSession().getAttribute("employee"));

            // 使用threadLocal存储当前用户id
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(request, response);
            return;
        }

        // 4-2. 判断移动端用户登录状态，如果已经登录，则直接放行
        if(request.getSession().getAttribute("user") != null) {
            log.info("用户已经登录，用户id为：{}", request.getSession().getAttribute("user"));

            // 使用threadLocal存储当前用户id
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(request, response);
            return;
        }

        log.info("用户未登录");
        //5. 如果未登录则返回未登录结果，通过输出流的方式向前端页面响应数据
        response.getWriter().write(JSON.toJSONString(Result.error("NOTLOGIN")));
        return;
    }

    /**
     * 路径匹配，检查本次请求是否放行
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
