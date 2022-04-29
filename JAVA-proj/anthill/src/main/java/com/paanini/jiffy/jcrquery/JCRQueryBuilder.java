package com.paanini.jiffy.jcrquery;

import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.encryption.api.HashiCorp;
import com.paanini.jiffy.exception.ProcessingException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Operand;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Athul Krishna N S
 * @since 30/04/20
 */
public class JCRQueryBuilder implements QueryBuilder{

    private static String SPACE = " ";
    private static String OPEN_BRACKET = "[";
    private static String CLOSE_BRACKET = "]";
    private static String ORDER_BY = "ORDER BY";
    private static String AND = "AND";
    private static String OR = "OR";
    private static String STRING = "STRING";
    private static String DATE = "DATE";
    private static String DATE_FORMAT= "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static String GREATER_THAN_OR_EQUAL_TO = ">=";
    private static String LESS_THAN_OR_EQUAL_TO = "<=";
    private static String EQUAL_TO = "=";

    public Query buildQuery(Session session, String nodePath,
                            QueryModel model) throws RepositoryException {
        String queryString = getQueryString(nodePath, model);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        if(Objects.nonNull(model.getLimit()) && Objects.nonNull(model.getOffset())) {
            query.setLimit(model.getLimit());
            query.setOffset(model.getOffset());
        }
        return query;
    }

    private String getQueryString(String nodePath, QueryModel model) {
        String baseQuery = getBaseQuery(nodePath);
        StringBuilder sb = new StringBuilder(baseQuery);
        if (model.getFilters().size() > 0) {
            sb.append(SPACE).append(AND).append(SPACE);
        }
        addFilters(model, sb);
        addOrder(model, sb);
        return sb.toString().trim();
    }

    private String getBaseQuery(String nodePath) {
        String baseQuery = "SELECT * FROM [nt:file] AS node WHERE ISCHILDNODE" +
                "(node,["+ nodePath + "])";
        return baseQuery;
    }

    private void addOrder(QueryModel model, StringBuilder sb) {
        Order order = model.getOrder();

        sb.append(ORDER_BY).append(SPACE).append(Common.NODE).append
        (OPEN_BRACKET);

        String column = getColumnMap().get(order.getColumn());
        validateColumnName(column);

        sb.append(column).append(CLOSE_BRACKET).append(SPACE);

        sb.append(order.getSortOrder() != null ?
                order.getSortOrder().name().toLowerCase() :
                SortOrder.ASC.toString().toLowerCase());
    }

    private void validateColumnName(String column) {
        if (Objects.isNull(column)) {
            throw new ProcessingException("column name not found");
        }
    }

    private void addFilters(QueryModel model, StringBuilder sb) {
        List<Filter> filters = model.getFilters();
        if (Objects.isNull(model.getFilters()) ||
                model.getFilters().isEmpty()) {
            return;
        }
        Map<String, List<Filter>> groupFilter = groupByFilterName(filters);
        addGroupCondition(sb, groupFilter);
    }

    private Map<String, List<Filter>> groupByFilterName(
            List<Filter> filters){
        Map<String, List<Filter>> groupFilter = new HashMap<>();
        filters.forEach(filter -> {
            if (!groupFilter.keySet().contains(filter.getColumn())) {
                List<Filter> filterList = new ArrayList<>();
                filterList.add(filter);
                groupFilter.put(filter.getColumn(), filterList);
            } else {
                groupFilter.get(filter.getColumn()).add(filter);
            }
        });
        return groupFilter;
    }

    private void addGroupCondition(StringBuilder sb,
            Map<String, List<Filter>> groupFilter) {

        String join = new StringBuilder().append(SPACE).append(OR)
                .append(SPACE).toString();

        Iterator<Map.Entry<String, List<Filter>>> iterator =
                groupFilter.entrySet().iterator();

        while (iterator.hasNext()) {
            sb.append("(");
            StringJoiner stringJoiner = new StringJoiner(join);
            for(Filter filter : iterator.next().getValue()) {
                stringJoiner.add(getFilterCriteria(filter));
            }
            sb.append(stringJoiner.toString());
            sb.append(")").append(SPACE);

            if (iterator.hasNext()) {
               sb.append(AND).append(SPACE);
            }
        }
    }

    private String getFilterCriteria(Filter filter) {
        StringBuilder sb = new StringBuilder();
        if (filter.getOperator().equals(Operator.BETWEEN)) {
            sb.append("(");
        }
        sb.append(Common.NODE).append(OPEN_BRACKET);
        String column = getColumnMap().get(filter.getColumn());
        validateColumnName(column);
        sb.append(column).append(CLOSE_BRACKET).append(SPACE);

        validateOperator(filter.getOperator());
        addFilterConditions(sb, filter);
        return sb.toString();
    }

    private void addFilterConditions(StringBuilder sb, Filter filter) {
        if (getTypeMap().get(filter.getColumn()).equals(STRING)) {
            if(filter.getOperator().equals(Operator.EQUAL)) {
                appendForEqualOperator(sb, filter);
            } else if(filter.getOperator().equals(Operator.LIKE)){
                appendForLikeOperator(sb, filter);
            } else if (filter.getOperator().equals(Operator.BETWEEN)) {
                if(filter.getValue() instanceof List &&
                        ((List) filter.getValue()).size() == 2) {
                    addFilterConditions(sb, new Filter(filter.getColumn(),
                            Operator.GREATER_THAN_OR_EQUAL_TO,
                            ((List) filter.getValue()).get(0)));

                    addColumn(sb, filter);

                    addFilterConditions(sb, new Filter(filter.getColumn(),
                            Operator.LESS_THAN_OR_EQUAL_TO,
                            ((List) filter.getValue()).get(1)));
                    sb.append(")");
                    return;
                } else  {
                    throw new ProcessingException("invalid  between filter for "+filter.getColumn());
                }
            }else {
                throw new ProcessingException("Operator not supported");
            }
        } else {
            appendForDate(sb, filter);
        }
    }

    private void appendForDate(StringBuilder sb, Filter filter){

        if (filter.getOperator().equals(Operator.GREATER_THAN_OR_EQUAL_TO)) {
            sb.append(GREATER_THAN_OR_EQUAL_TO);
        } else if (filter.getOperator().equals(Operator.LESS_THAN_OR_EQUAL_TO)){
            sb.append(LESS_THAN_OR_EQUAL_TO);
        } else if (filter.getOperator().equals(Operator.EQUAL)) {
            sb.append(EQUAL_TO);
        } else if (filter.getOperator().equals(Operator.BETWEEN)) {
            if(filter.getValue() instanceof List &&
                    ((List) filter.getValue()).size() == 2) {
                appendForDate(sb, new Filter(filter.getColumn(),
                        Operator.GREATER_THAN_OR_EQUAL_TO,
                        ((List) filter.getValue()).get(0)));

                addColumn(sb, filter);

                appendForDate(sb, new Filter(filter.getColumn(),
                        Operator.LESS_THAN_OR_EQUAL_TO,
                        ((List) filter.getValue()).get(1)));
                sb.append(")");
                return;
            } else  {
                throw new ProcessingException("invalid  between filter for dates");
            }
        } else {
            throw new ProcessingException("Operator "+
                    filter.getOperator().name()+" not supported");
        }

        SimpleDateFormat sdf = new
                SimpleDateFormat(DATE_FORMAT);

        sb.append(SPACE).append("CAST('");
        sb.append(sdf.format(getDate(filter)));
        sb.append("' AS DATE)").append(SPACE);
    }

    private Date getDate(Filter filter){
        long milliseconds;
        try {
            milliseconds = Long.parseLong(filter.getValue().toString());
        } catch (NumberFormatException e) {
            throw new ProcessingException(e.getMessage());
        }
        return new Date(milliseconds);
    }


    private void addColumn(StringBuilder sb, Filter filter) {
        sb.append(SPACE).append(AND).append(SPACE);
        sb.append(Common.NODE).append(OPEN_BRACKET);
        sb.append(getColumnMap().get(filter.getColumn())).append(CLOSE_BRACKET);
    }

    private void appendForEqualOperator(StringBuilder sb, Filter filter) {
        sb.append(EQUAL_TO).append(SPACE);
        sb.append("'").append(filter.getValue()).append("'");
        sb.append(SPACE);
    }

    private void appendForLikeOperator(StringBuilder sb, Filter filter) {
        sb.append(filter.getOperator().name());
        sb.append(SPACE).append("'%");
        sb.append(filter.getValue());
        sb.append("%'").append(SPACE);
    }

    private void validateOperator(Operator operator) {
        if( !Arrays.asList(Operator.values()).contains(operator)) {
            throw new ProcessingException("Operation "+ operator.name()
                    +" not supported");
        }
    }

    private Map<String, String> getColumnMap() {
        Map<String, String> columnMap = new HashMap<>();
        columnMap.put("name", "jcr:title");
        columnMap.put("path", "jcr:path");
        columnMap.put(Common.OWNER_COLUMN, Common.OWNER_COLUMN);
        columnMap.put("type", "type");
        columnMap.put("lastModified", "jcr:lastModified");
        columnMap.put("id", "jcr:uuid");
        columnMap.put("created", "jcr:created");
        columnMap.put(Common.TABLE_TYPE, Common.TABLE_TYPE);
        columnMap.put(Common.CREATED_BY, Common.CREATED_BY);
        columnMap.put(HashiCorp.TABLE_MODE, HashiCorp.TABLE_MODE);
        columnMap.put(Common.TABLE_ALIAS_NAME, Common.TABLE_ALIAS_NAME);
        columnMap.put(Common.SUB_TYPE, Common.SUB_TYPE);
        return columnMap;
    }

    private Map<String, String> getTypeMap() {
        Map<String, String> typeMap = new HashMap<>();
        typeMap.put("name", STRING);
        typeMap.put("path", STRING);
        typeMap.put(Common.OWNER_COLUMN, STRING);
        typeMap.put("type", STRING);
        typeMap.put("id", STRING);
        typeMap.put("lastModified", DATE);
        typeMap.put(Common.TABLE_TYPE, STRING);
        typeMap.put("created", DATE);
        typeMap.put(Common.CREATED_BY, STRING);
        typeMap.put(HashiCorp.TABLE_MODE,STRING);
        typeMap.put(Common.TABLE_ALIAS_NAME,STRING);
        typeMap.put(Common.SUB_TYPE,STRING);
        return typeMap;
    }
}
