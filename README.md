# deepinbbs-rss

deepin新版论坛没有rss订阅源，有大佬@dz_123写了个python程序来获取。[^1]

[^1]:[[使用交流] 换了新论坛，我的rss又挂了一个](https://bbs.deepin.org/post/205332)

但是我不喜欢python，尤其不喜欢它的包管理。而且看上去这段代码没有抓取摘要的能力，于是就自己写了一个、

打成了fatjar，有jre就可以用了。

## 使用方法

1. 下载release里面的jar包
2. java -jar deepinbbs-rss-fat.jar --port 8080
3. 到rss阅读器添加订阅。
    - 全部：`http://localhost:8080/all`
    - 热门：`http://localhost:8080/hot`
    - 精华：`http://localhost:8080/highlight`
    
这里的`--port参数是绑定的端口号，默认8080`

你也可以使用`--save`参数保存xml文件到本地。

`java -jar deepinbbs-rss-fat.jar --save /Users/zero/.config/kfeed/deepin`