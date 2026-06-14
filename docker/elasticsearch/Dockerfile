FROM elasticsearch:8.13.0

# 安装拼音分词插件和 IK 中文分词插件
RUN elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-pinyin/8.13.0 \
    && elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.13.0
