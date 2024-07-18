package com.netease.open.libpoco.sdk;

import android.annotation.SuppressLint;
import android.view.accessibility.AccessibilityNodeInfo;

import com.netease.open.libpoco.Node;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by adolli on 2017/7/10.
 */

@SuppressLint("NewApi")
public abstract class AbstractDumper implements IDumper<AbstractNode> {
    public static String TAG = "AbstractDumper";

    public JSONObject dumpHierarchy() throws JSONException {
        return this.dumpHierarchy(true);
    }

    public JSONObject dumpHierarchy(boolean onlyVisibleNode) throws JSONException {
        Set<AccessibilityNodeInfo> trace = new HashSet<>();
        return this.dumpHierarchyImpl(trace, this.getRoot(), onlyVisibleNode);
    }

    public JSONObject dumpHierarchyImpl(Set<AccessibilityNodeInfo> trace, AbstractNode node, boolean onlyVisibleNode) throws JSONException {
        if (node == null) {
            // return if still null
            return null;
        }

        JSONObject payload = new JSONObject();
        for (Map.Entry<String, Object> attr : node.enumerateAttrs().entrySet()) {
            payload.put(attr.getKey(), attr.getValue());
        }

        JSONObject result = new JSONObject();
        JSONArray children = new JSONArray();
        for (AbstractNode child : node.getChildren()) {
            if (child instanceof Node) {
                if (trace.contains(((Node) child).node)) {
                    continue;
                }
                trace.add(((Node) child).node);
            }
            if (!onlyVisibleNode || (boolean) child.getAttr("visible")) {
                children.put(this.dumpHierarchyImpl(trace, child, onlyVisibleNode));
            }
            else if(String.valueOf(child.getAttr("type")).endsWith(".WebView")){
                // 这个改动主要是一些WebView如果使用了tbs引擎，会因为isVisibleToUser返回false而导致无法获取节点
                // 测试时使用了腾讯自选股app的基金页面进行复现
                children.put(this.dumpHierarchyImpl(trace, child, false));
            }
        }
        if (children.length() > 0) {
            result.put("children", children);
        }

        result.put("name", node.getAttr("name"));
        result.put("payload", payload);

        return result;
    }

}
