package com.netease.open.libpoco.sdk;

import android.annotation.SuppressLint;
import android.view.accessibility.AccessibilityNodeInfo;

import com.netease.open.libpoco.Node;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
        Map<AccessibilityNodeInfo, Integer> trace = new HashMap<>();
        return this.dumpHierarchyImpl(trace, this.getRoot(), onlyVisibleNode);
    }

    public JSONObject dumpHierarchyImpl(Map<AccessibilityNodeInfo, Integer> trace, AbstractNode node, boolean onlyVisibleNode) throws JSONException {
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
                int count = trace.getOrDefault(((Node) child).node, 0);
                if (count > 3) {
                    continue;
                }
                trace.put(((Node) child).node, count + 1);
            }
            if (!onlyVisibleNode || (boolean) child.getAttr("visible")) {
                children.put(this.dumpHierarchyImpl(trace, child, onlyVisibleNode));
            } else if (String.valueOf(child.getAttr("type")).endsWith(".WebView")) {
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

    public boolean hasChildren() {
        AbstractNode root = this.getRoot();
        for (AbstractNode child : root.getChildren()) {
            if ((boolean) child.getAttr("visible") || String.valueOf(child.getAttr("type")).endsWith(".WebView")) {
                return true;
            }
        }
        return false;
    }

}
