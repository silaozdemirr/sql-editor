import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ConnectionPanel from './ConnectionPanel';

describe('ConnectionPanel Bileşeni', () => {
  it('Bağlan butonunu ve form alanlarını doğru render etmeli', () => {
    render(<ConnectionPanel onConnect={vi.fn()} />);
    
    // Veritabanı Türü seçenekleri var mı?
    expect(screen.getAllByText(/MySQL/i)[0]).toBeInTheDocument();
    
    // Sunucu adresi varsayılan localhost mu?
    const hostInput = screen.getByLabelText(/Host/i);
    expect(hostInput).toHaveValue('localhost');
    
    // Port varsayılan 3306 mı?
    const portInput = screen.getByLabelText(/Port/i);
    expect(portInput).toHaveValue(3306);
    
    // Bağlan butonu ekranda mı?
    const connectButton = screen.getByRole('button', { name: /bağlan/i });
    expect(connectButton).toBeInTheDocument();
  });
});
