import React, { useEffect, useState } from 'react';
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  Handle,
  Position,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { getErd } from '../api/schemaApi';

const TableNode = ({ data }) => {
  return (
    <div style={{ background: '#fff', border: '1px solid #1a202c', borderRadius: '6px', minWidth: '220px', fontSize: '13px', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)', overflow: 'hidden', fontFamily: 'system-ui, sans-serif' }}>
      <Handle type="target" position={Position.Left} style={{ background: '#4a5568', width: '8px', height: '8px' }} />
      <div style={{ background: '#2d3748', color: '#fff', padding: '10px 12px', fontWeight: 'bold', textAlign: 'center', letterSpacing: '0.5px' }}>
        {data.name}
      </div>
      <div style={{ padding: '8px 12px', display: 'flex', flexDirection: 'column', gap: '6px', background: '#f7fafc' }}>
        {data.columns.map(col => (
          <div key={col.name} style={{ display: 'flex', justifyContent: 'space-between', padding: '4px 0', borderBottom: '1px solid #edf2f7' }}>
            <span style={{ fontWeight: col.primaryKey ? '600' : '500', color: col.primaryKey ? '#2b6cb0' : '#4a5568' }}>
              {col.primaryKey && <span title="Primary Key" style={{ marginRight: '4px' }}>🔑</span>}
              {col.foreignKey && <span title="Foreign Key" style={{ marginRight: '4px' }}>🔗</span>}
              {col.name}
            </span>
            <span style={{ color: '#718096', marginLeft: '16px', fontSize: '12px' }}>{col.fullType || col.dataType}</span>
          </div>
        ))}
      </div>
      <Handle type="source" position={Position.Right} style={{ background: '#4a5568', width: '8px', height: '8px' }} />
    </div>
  );
};

const nodeTypes = {
  tableNode: TableNode,
};

export default function ErdViewer({ connectionToken, database }) {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchErd = async () => {
      try {
        const data = await getErd(connectionToken, database);
        
        let x = 50;
        let y = 50;
        
        const newNodes = data.tables.map((table, index) => {
          const posX = x + (index % 3) * 350;
          const posY = y + Math.floor(index / 3) * 300;
          return {
            id: table.name,
            type: 'tableNode',
            position: { x: posX, y: posY },
            data: { name: table.name, columns: table.columns },
          };
        });

        const newEdges = data.edges.map((edge, index) => {
          return {
            id: `e-${edge.sourceTable}-${edge.targetTable}-${index}`,
            source: edge.sourceTable,
            target: edge.targetTable,
            animated: true,
            style: { stroke: '#2b6cb0', strokeWidth: 2.5 },
          };
        });

        setNodes(newNodes);
        setEdges(newEdges);
      } catch (err) {
        console.error("ERD yüklenemedi", err);
      } finally {
        setLoading(false);
      }
    };

    if (connectionToken) {
      fetchErd();
    }
  }, [connectionToken, database, setNodes, setEdges]);

  if (loading) return <div style={{ padding: '20px' }}>ER Diyagramı yükleniyor...</div>;

  return (
    <div style={{ width: '100%', height: '100%', background: '#fafafa', flex: 1 }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
      >
        <MiniMap />
        <Controls />
        <Background color="#aaa" gap={16} />
      </ReactFlow>
    </div>
  );
}
