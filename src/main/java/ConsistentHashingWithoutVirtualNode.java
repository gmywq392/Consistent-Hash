import java.util.*;

public class ConsistentHashingWithoutVirtualNode {
    /**
     * 集群地址列表
     */
    private static final String[] groups = {
            "192.168.0.0:111", "192.168.0.1:111", "192.168.0.2:111", "192.168.0.3:111", "192.168.0.4:111"
    };
    /**
     * 真实集群列表
     */
    private static final List<String> realGroups = new LinkedList<>();

    /**
     * 用于保存 hash 环上的节点
     */
    private static final SortedMap<Integer, String> virtualNodes = new TreeMap<>();
    private static final int VIRTUAL_NODE_NUM = 1000;

    /**
     * 用于保存hash环上的节点
     */
    static {
        // 先添加真实节点列表
        realGroups.addAll(Arrays.asList(groups));

        // 将虚拟节点映射到 hash 环上
        // 使用红黑树实现，插入效率比较差，单实查找效率高
        for (String realGroup : realGroups) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(realGroup, i);

                int hash = HashUtil.getHash(virtualNodeName);
                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }

    private static String getVirtualNodeName(String realGroup, int num) {
        return realGroup + "&&VN" + num;
    }

    private static String getRealNodeName(String virtualName) {
        return virtualName.split("&&")[0];
    }


    /**
     * 计算对应的 widget 加载在哪个 group 上
     */
    private static String getServer(String widgetKey) {
        int hash = HashUtil.getHash(widgetKey);
        // 只取出所有大于该hash值的部分而不必遍历整个tree
        SortedMap<Integer, String> subMap = virtualNodes.tailMap(hash);
        String virtualNodeName;
        if (subMap.isEmpty()) {
            // hash值在最尾部，应该映射到第一个group上
            virtualNodeName = virtualNodes.get(virtualNodes.firstKey());
        } else {
            virtualNodeName = subMap.get(subMap.firstKey());
        }
        return getRealNodeName(virtualNodeName);
    }

    private static void refreshHashCircle() {
        // 当集群变动时，刷新hash环，其余的集群在hash环上的位置不会发生变动
        virtualNodes.clear();
        for (String realGroup : realGroups) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                String virtualNodeName = getVirtualNodeName(realGroup, i);
                int hash = HashUtil.getHash(virtualNodeName);

                System.out.println("[" + virtualNodeName + "] launched @ " + hash);
                virtualNodes.put(hash, virtualNodeName);
            }
        }
    }

    private static void addGroups(String identifier) {
        realGroups.add(identifier);
        refreshHashCircle();
    }

    private static void removeGroups(String identifier) {
        int i = 0;
        for (String realGroup : realGroups) {
            if (realGroup.equals(identifier)) {
                realGroups.remove(i);
            }
            i++;
        }
        refreshHashCircle();
    }

    public static void main(String[] args) {
        // 生成随机数进行测试
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < 100000; i++) {
            Integer widgetId = i;
            String server = getServer(widgetId.toString());
            map.putIfAbsent(server, 1);
            map.put(server, map.get(server) + 1);
            if (i == 595) {
                addGroups("192.168.0.5:111");
            }else if (i == 9852){
                removeGroups("192.168.0.4:111");
            }
        }
        map.forEach((k, v) -> {
            System.out.println("group " + k + ": " + v + "(" + v / 1000.0D + "%)");
        });
    }

}
