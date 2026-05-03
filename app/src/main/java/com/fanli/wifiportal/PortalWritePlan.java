package com.fanli.wifiportal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class PortalWritePlan {
    final String title;
    final List<Item> items;

    PortalWritePlan(String title, List<Item> items) {
        this.title = title;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    List<String> commands() {
        List<String> commands = new ArrayList<>();
        for (Item item : items) {
            commands.add(item.command);
        }
        return commands;
    }

    String describeForReview() {
        StringBuilder builder = new StringBuilder();
        for (Item item : items) {
            builder.append(item.setting.label())
                    .append('\n')
                    .append("来源: ").append(item.source)
                    .append('\n')
                    .append("原始: ").append(item.original == null ? "(未备份)" : item.original.display())
                    .append('\n')
                    .append("当前: ").append(item.current == null ? "(未读取)" : item.current.display())
                    .append('\n')
                    .append("目标: ").append(item.target.display())
                    .append('\n')
                    .append("命令: ").append(item.command)
                    .append("\n\n");
        }
        return builder.toString();
    }

    List<Item> mismatches(java.util.Map<String, PortalValue> current) {
        List<Item> mismatches = new ArrayList<>();
        for (Item item : items) {
            PortalValue actual = current.get(item.setting.id());
            if (actual == null || !actual.same(item.target)) {
                mismatches.add(item);
            }
        }
        return mismatches;
    }

    static final class Item {
        final PortalSetting setting;
        final PortalValue original;
        final PortalValue current;
        final PortalValue target;
        final String command;
        final String source;

        Item(PortalSetting setting, PortalValue original, PortalValue current, PortalValue target, String command, String source) {
            this.setting = setting;
            this.original = original;
            this.current = current;
            this.target = target;
            this.command = command;
            this.source = source;
        }
    }
}
